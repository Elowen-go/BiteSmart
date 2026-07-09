package com.ws.bitesmart.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置
 */
@Configuration
@MapperScan("com.ws.bitesmart.mapper")
public class MyBatisConfig {
    // MyBatis 配置已在 application.yml 中完成
    // 此配置类仅用于 Mapper 扫描路径声明
}
