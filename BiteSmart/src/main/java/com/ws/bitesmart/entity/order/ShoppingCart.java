package com.ws.bitesmart.entity.order;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 购物车表 实体类
 *
 * 对应 shopping_cart 表。
 * 用户添加菜品或套餐到购物车，支持定制信息。
 * itemType：10-菜品 20-套餐
 */
@Data
public class ShoppingCart {

    private Long id;
    private Long userId;

    /** 商品类型：10-菜品 20-套餐 */
    private Integer itemType;

    /** 菜品ID（itemType=10时） */
    private Long dishId;

    /** 套餐ID（itemType=20时） */
    private Long comboId;

    /** 数量 */
    private Integer quantity;

    /** 是否选中：1-选中 0-未选 */
    private Integer selected;

    /** 定制信息（如套餐换菜）JSON */
    private String customizationJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 购物车展示快照，由服务层查询商品后填充，不落库。 */
    private String dishName;
    private String comboName;
    private String dishImage;
    private String comboImage;
    private java.math.BigDecimal price;
}
