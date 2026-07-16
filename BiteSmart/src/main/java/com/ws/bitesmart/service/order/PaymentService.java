package com.ws.bitesmart.service.order;

import com.alibaba.fastjson2.JSON;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.order.ComboCustomizationSnapshot;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.PaymentLog;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.PaymentLogMapper;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import com.ws.bitesmart.service.system.OperateLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrdersMapper ordersMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final DishMapper dishMapper;
    private final OrderItemMapper orderItemMapper;
    private final ComboDishRelMapper comboDishRelMapper;
    private final OperateLogService operateLogService;
    private final MerchantFinanceService merchantFinanceService;

    @Transactional
    public void pay(String orderNo, Integer payMethod) {
        Orders order = getUnpaidOrder(orderNo);
        String transactionNo = "MOCK" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();
        int affected = ordersMapper.updateStatusWithLock(
                order.getId(), 10, 20, payMethod, order.getPayAmount(), LocalDateTime.now(),
                null, null, null, null);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "Order status changed, payment failed");
        }
        PaymentLog paymentLog = createPaymentLog(order, orderNo, payMethod, transactionNo, LocalDateTime.now());
        paymentLogMapper.insert(paymentLog);
        merchantFinanceService.recordPaymentIncome(order, paymentLog);
        deductLockedStock(order.getId());
        operateLogService.record(order.getUserId(), null, null,
                "Mock payment", "PaymentService.pay", null, orderNo, null, null, null);
    }

    @Transactional
    public void payWithExternalResult(String orderNo, Integer payMethod, String transactionNo,
                                      BigDecimal paidAmount, LocalDateTime paidAt) {
        Orders order = ordersMapper.findByOrderNo(orderNo);
        if (order == null) throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        if (order.getOrderStatus() == 20) return;
        if (order.getOrderStatus() != 10) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "Order does not allow payment");
        }
        if (paidAmount == null || order.getPayAmount().compareTo(paidAmount) != 0) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_FAILED, "Payment amount mismatch");
        }
        LocalDateTime paymentTime = paidAt == null ? LocalDateTime.now() : paidAt;
        int affected = ordersMapper.updateStatusWithLock(
                order.getId(), 10, 20, payMethod, order.getPayAmount(), paymentTime,
                null, null, null, null);
        if (affected == 0) return;

        PaymentLog paymentLog = createPaymentLog(order, orderNo, payMethod, transactionNo, paymentTime);
        paymentLogMapper.insert(paymentLog);
        merchantFinanceService.recordPaymentIncome(order, paymentLog);
        deductLockedStock(order.getId());
        operateLogService.record(order.getUserId(), null, null,
                "Alipay payment", "PaymentService.payWithExternalResult", null, orderNo, null, null, null);
    }

    private Orders getUnpaidOrder(String orderNo) {
        Orders order = ordersMapper.findByOrderNo(orderNo);
        if (order == null) throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        if (order.getOrderStatus() != 10) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "Order does not allow payment");
        }
        return order;
    }

    private PaymentLog createPaymentLog(Orders order, String orderNo, Integer payMethod,
                                       String transactionNo, LocalDateTime paymentTime) {
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setId(SnowflakeUtil.generate());
        paymentLog.setOrderId(order.getId());
        paymentLog.setOrderNo(orderNo);
        paymentLog.setPayMethod(payMethod);
        paymentLog.setTransactionNo(transactionNo);
        paymentLog.setPayAmount(order.getPayAmount());
        paymentLog.setPayStatus(20);
        paymentLog.setPayTime(paymentTime);
        return paymentLog;
    }

    private void deductLockedStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getItemType() == 10 && item.getDishId() != null) {
                dishMapper.deductLockedStock(item.getDishId(), item.getQuantity());
            } else if (item.getItemType() == 20 && item.getComboId() != null) {
                for (ComboCustomizationSnapshot.SelectedDishItem rel : resolveSnapshotItems(item)) {
                    dishMapper.deductLockedStock(rel.getDishId(), rel.getQuantity() * item.getQuantity());
                }
            }
        }
    }

    private List<ComboCustomizationSnapshot.SelectedDishItem> resolveSnapshotItems(OrderItem item) {
        if (item.getSnapshotNutritionJson() != null && !item.getSnapshotNutritionJson().isEmpty()) {
            ComboCustomizationSnapshot snapshot = JSON.parseObject(item.getSnapshotNutritionJson(),
                    ComboCustomizationSnapshot.class);
            if (snapshot != null && snapshot.getItems() != null && !snapshot.getItems().isEmpty()) {
                return snapshot.getItems();
            }
        }
        List<ComboCustomizationSnapshot.SelectedDishItem> items = new ArrayList<>();
        List<ComboDishRel> rels = comboDishRelMapper.findByComboId(item.getComboId());
        for (ComboDishRel rel : rels) {
            ComboCustomizationSnapshot.SelectedDishItem selected = new ComboCustomizationSnapshot.SelectedDishItem();
            selected.setDishId(rel.getDishId());
            selected.setQuantity(rel.getQuantity() == null ? 1 : rel.getQuantity());
            items.add(selected);
        }
        return items;
    }
}
