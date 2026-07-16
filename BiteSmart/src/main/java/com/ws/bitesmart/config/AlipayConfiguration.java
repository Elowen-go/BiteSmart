package com.ws.bitesmart.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlipayProperties.class)
public class AlipayConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "alipay", name = "enabled", havingValue = "true")
    public AlipayClient alipayClient(AlipayProperties properties) {
        require(properties.getAppId(), "ALIPAY_APP_ID");
        require(properties.getPrivateKey(), "ALIPAY_PRIVATE_KEY");
        require(properties.getPublicKey(), "ALIPAY_PUBLIC_KEY");
        return new DefaultAlipayClient(
                properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getPrivateKey(),
                properties.getFormat(),
                properties.getCharset(),
                properties.getPublicKey(),
                properties.getSignType()
        );
    }

    private void require(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("支付宝沙箱已启用，但未配置 " + envName);
        }
    }
}
