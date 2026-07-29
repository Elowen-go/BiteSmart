package com.ws.bitesmart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    private boolean enabled;
    private String appId;
    private String privateKey;
    private String publicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String membershipReturnUrl;
    private String signType = "RSA2";
    private String charset = "UTF-8";
    private String format = "json";
}
