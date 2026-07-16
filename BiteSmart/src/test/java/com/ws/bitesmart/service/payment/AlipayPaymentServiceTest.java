package com.ws.bitesmart.service.payment;

import com.alipay.api.AlipayClient;
import com.ws.bitesmart.config.AlipayProperties;
import com.ws.bitesmart.service.order.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlipayPaymentServiceTest {

    @Mock private ObjectProvider<AlipayClient> clientProvider;
    @Mock private PaymentService paymentService;

    @Test
    void callbackVerificationKeepsTheSignParameterForSdkVerification() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        AlipayProperties properties = new AlipayProperties();
        properties.setEnabled(true);
        properties.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        properties.setCharset("UTF-8");
        properties.setSignType("RSA2");
        when(clientProvider.getIfAvailable()).thenReturn(mock(AlipayClient.class));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", "ORD-1");
        params.put("trade_no", "TRADE-1");
        params.put("total_amount", "10.00");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign_type", "RSA2");
        params.put("sign", sign(params, keyPair));

        AlipayPaymentService service = new AlipayPaymentService(clientProvider, properties, paymentService);

        assertThatCode(() -> service.handleNotify(params)).doesNotThrowAnyException();
        verify(paymentService).payWithExternalResult(
                eq("ORD-1"), eq(10), eq("TRADE-1"), eq(new BigDecimal("10.00")), any(LocalDateTime.class));
    }

    private String sign(Map<String, String> params, KeyPair keyPair) throws Exception {
        String content = params.entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
