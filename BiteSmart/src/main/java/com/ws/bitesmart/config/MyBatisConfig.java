package com.ws.bitesmart.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置
 *
 * 每新增一个业务域模块，需要在 @MapperScan 中加上对应的 mapper 子包路径。
 */
@Configuration
@MapperScan({
        "com.ws.bitesmart.mapper.user",
        "com.ws.bitesmart.mapper.health",
        "com.ws.bitesmart.mapper.file",
        "com.ws.bitesmart.mapper.ai",
        "com.ws.bitesmart.mapper.dish",
        "com.ws.bitesmart.mapper.merchant",
        "com.ws.bitesmart.mapper.order"
})
public class MyBatisConfig {
    // MyBatis 配置已在 application.yml 中完成
    // 此配置类仅用于 Mapper 扫描路径声明
}
