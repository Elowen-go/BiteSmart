package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品表 实体类
 *
 * 对应 dish 表。
 * 商家上架菜品时填基本信息+营养信息。
 * suitable_for 是 JSON，存 ["减肥","增肌","控糖","儿童","老人"] 等标签。
 */
@Data
public class Dish {

    private Long id;
    private Long merchantId;
    private Long categoryId;
    private String dishName;
    private String dishImage;
    private BigDecimal price;
    private BigDecimal originalPrice;

    /** 实际库存 */
    private Integer stock;

    /** 锁定库存（下单未支付） */
    private Integer lockStock;

    /** 库存预警阈值 */
    private Integer minStockWarning;

    /** 总销量 */
    private Integer salesCount;

    /** 真实销量 */
    private Integer salesReal;

    /** 单位（份） */
    private String unit;

    private String description;

    /** 适宜人群 JSON */
    private String suitableFor;

    /** 热量（大卡） */
    private Integer calories;

    /** 蛋白质（g） */
    private BigDecimal protein;

    /** 脂肪（g） */
    private BigDecimal fat;

    /** 碳水化合物（g） */
    private BigDecimal carbs;

    /** 状态：10-上架 20-下架 30-售罄 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
