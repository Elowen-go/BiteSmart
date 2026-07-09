package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐表 实体类
 *
 * 对应 combo 表。
 * 一个套餐包含多个菜品，通过 combo_dish_rel 关联。
 * 套餐可以设置可替换菜品池，用户下单时可以换菜。
 */
@Data
public class Combo {

    private Long id;
    private Long merchantId;
    private String comboName;
    private String comboImage;
    private BigDecimal price;
    private BigDecimal originalPrice;

    /** 套餐类型：10-减脂 20-增肌 30-控糖 40-会员专属 */
    private Integer comboType;

    /** 适宜人群 JSON */
    private String suitableFor;

    /** 套餐总热量 */
    private Integer totalCalories;

    /** 套餐总蛋白质 */
    private BigDecimal totalProtein;

    /** 套餐总脂肪 */
    private BigDecimal totalFat;

    /** 套餐总碳水 */
    private BigDecimal totalCarbs;

    private String description;

    /** 可替换菜品池（菜品ID数组 JSON） */
    private String replaceableDishPool;

    /** 最大可替换菜品数量（0表示不可换） */
    private Integer maxReplaceCount;

    /** 状态：10-上架 20-下架 */
    private Integer status;

    /** 套餐销量 */
    private Integer salesCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
