package com.ws.bitesmart.entity.system;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置表 实体类
 *
 * 对应 system_config 表。
 * key-value 结构，按 group 分组，支持敏感标记。
 * 用于存储系统运行参数，如会员价格、配送费、运营配置等。
 */
@Data
public class SysConfig {

    private Long id;

    /** 配置键（唯一） */
    private String configKey;

    /** 配置值（JSON 或字符串） */
    private String configValue;

    /** 配置分组 */
    private String configGroup;

    /** 配置说明 */
    private String description;

    /** 是否敏感配置（前端不可见） */
    private Integer isSensitive;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
