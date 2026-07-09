package com.ws.bitesmart.entity.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表（快照存储）实体类
 *
 * 对应 order_item 表。
 * 下单时对商品信息做快照，确保历史订单不会因商品信息变更而丢失数据。
 * itemType：10-菜品 20-套餐
 */
@Data
public class OrderItem {

    private Long id;
    private Long orderId;

    /** 商品类型：10-菜品 20-套餐 */
    private Integer itemType;

    /** 菜品ID（冗余用于统计） */
    private Long dishId;

    /** 套餐ID（冗余用于统计） */
    private Long comboId;

    /** 快照名称 */
    private String snapshotName;

    /** 快照图片 */
    private String snapshotImage;

    /** 快照单价 */
    private BigDecimal snapshotPrice;

    /** 快照热量 */
    private Integer snapshotCalories;

    /** 快照蛋白质（g） */
    private BigDecimal snapshotProtein;

    /** 快照脂肪（g） */
    private BigDecimal snapshotFat;

    /** 快照碳水（g） */
    private BigDecimal snapshotCarbs;

    /** 完整营养JSON快照 */
    private String snapshotNutritionJson;

    /** 数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal subTotal;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
