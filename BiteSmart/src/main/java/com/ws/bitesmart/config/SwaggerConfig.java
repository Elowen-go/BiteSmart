package com.ws.bitesmart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 配置
 *
 * 接口文档访问地址：http://localhost:8080/swagger-ui/index.html
 * OpenAPI JSON：http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BiteSmart API 接口文档")
                        .version("1.0")
                        .description("智能健康膳食管理平台后端接口")
                        .contact(new Contact().name("BiteSmart")));
    }
}
