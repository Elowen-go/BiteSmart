package com.ws.bitesmart.entity.health;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录表 实体类
 *
 * 对应 diet_record 表。
 * 用户每天记录自己吃了什么，按餐次区分。
 * 如果是平台订单导入的，source_type=10 且有 order_item_id。
 */
@Data
public class DietRecord {

    private Long id;
    private Long userId;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 餐次：10-早餐 20-午餐 30-晚餐 40-加餐 */
    private Integer mealType;

    /** 食物名称 */
    private String foodName;

    /** 热量（大卡） */
    private Integer calories;

    /** 蛋白质（g） */
    private BigDecimal protein;

    /** 脂肪（g） */
    private BigDecimal fat;

    /** 碳水（g） */
    private BigDecimal carbs;

    /** 来源：10-平台订单自动 20-用户手动添加 */
    private Integer sourceType;

    /** 关联订单明细ID */
    private Long orderItemId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
