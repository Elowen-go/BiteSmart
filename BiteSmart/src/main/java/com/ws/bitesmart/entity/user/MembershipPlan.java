package com.ws.bitesmart.entity.user;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员套餐定义表 实体类
 *
 * 对应 membership_plan 表。
 * 由管理员配置，用户在前端看到的就是这些套餐选项。
 * benefits 字段是 JSON，存的是权益描述。
 */
@Data
public class MembershipPlan {

    private Long id;

    /** 套餐名称（月卡/季卡/年卡） */
    private String planName;

    /** 套餐类型：10-月卡 20-季卡 30-年卡 */
    private Integer planType;

    /** 售价 */
    private BigDecimal price;

    /** 原价（展示折扣） */
    private BigDecimal originalPrice;

    /** 有效天数（月卡30天） */
    private Integer validDays;

    /** 权益描述 JSON，如 {"ai_advanced":true,"exclusive_combo":true,"discount":0.8} */
    private String benefits;

    /** 状态：10-上架 20-下架 */
    private Integer status;

    /** 排序序号 */
    private Integer sortOrder;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
