package com.ws.bitesmart.service.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.ws.bitesmart.config.AlipayProperties;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.PaymentLog;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.entity.user.MembershipPaymentOrder;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.service.order.PaymentService;
import com.ws.bitesmart.service.user.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlipayPaymentService {
    private final ObjectProvider<AlipayClient> clientProvider;
    private final AlipayProperties properties;
    private final PaymentService paymentService;
    private final MembershipService membershipService;

    public boolean isEnabled() {
        return properties.isEnabled() && clientProvider.getIfAvailable() != null;
    }

    public String createPagePay(Orders order) {
        return createPagePay(order.getOrderNo(), order.getPayAmount(),
                "BiteSmart order " + order.getOrderNo(), properties.getReturnUrl());
    }

    public String createPagePay(MembershipPaymentOrder order) {
        return createPagePay(order.getOrderNo(), order.getPayAmount(),
                "BiteSmart membership " + order.getOrderNo(), properties.getMembershipReturnUrl());
    }

    private String createPagePay(String outTradeNo, BigDecimal totalAmount,
                                 String subject, String returnUrl) {
        AlipayClient client = clientProvider.getIfAvailable();
        if (!isEnabled() || client == null) {
            throw new BusinessException("Alipay sandbox is not configured");
        }
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(totalAmount.toPlainString());
        model.setSubject(subject);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setBizModel(model);
        request.setReturnUrl(returnUrl == null || returnUrl.isBlank()
                ? properties.getReturnUrl() : returnUrl);
        if (properties.getNotifyUrl() != null && !properties.getNotifyUrl().isBlank()) {
            request.setNotifyUrl(properties.getNotifyUrl());
        }
        try {
            return client.pageExecute(request).getBody();
        } catch (AlipayApiException ex) {
            throw new BusinessException("Failed to create Alipay payment");
        }
    }

    public void handleNotify(Map<String, String> params) {
        if (!isEnabled()) {
            throw new BusinessException("Alipay sandbox is disabled");
        }
        try {
            boolean valid = AlipaySignature.rsaCheckV1(
                    params, properties.getPublicKey(), properties.getCharset(), properties.getSignType());
            if (!valid) {
                throw new BusinessException("Invalid Alipay callback signature");
            }
        } catch (AlipayApiException ex) {
            throw new BusinessException("Failed to verify Alipay callback");
        }
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return;
        }
        String outTradeNo = params.get("out_trade_no");
        BigDecimal paidAmount = new BigDecimal(params.get("total_amount"));
        if (outTradeNo != null && outTradeNo.startsWith(MembershipService.PAYMENT_ORDER_PREFIX)) {
            membershipService.completePayment(outTradeNo, params.get("trade_no"), paidAmount, LocalDateTime.now());
            return;
        }
        paymentService.payWithExternalResult(
                outTradeNo, 10, params.get("trade_no"), paidAmount, LocalDateTime.now());
    }

    public void refund(Orders order, RefundApplication refund, PaymentLog paymentLog) {
        AlipayClient client = clientProvider.getIfAvailable();
        if (!isEnabled() || client == null) {
            throw new BusinessException("Alipay sandbox is not configured");
        }

        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(order.getOrderNo());
        if (paymentLog != null && paymentLog.getTransactionNo() != null
                && !paymentLog.getTransactionNo().isBlank()) {
            model.setTradeNo(paymentLog.getTransactionNo());
        }
        model.setRefundAmount(refund.getRefundAmount().setScale(2).toPlainString());
        model.setOutRequestNo("REFUND-" + refund.getId());
        model.setRefundReason(refund.getRefundReason());

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);
        try {
            AlipayTradeRefundResponse response = client.execute(request);
            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Alipay refund failed");
            }
        } catch (AlipayApiException ex) {
            throw new BusinessException("Alipay refund failed");
        }
    }
}
