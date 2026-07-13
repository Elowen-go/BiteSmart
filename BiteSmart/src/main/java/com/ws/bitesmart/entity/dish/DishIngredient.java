package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品-食材关联实体类
 *
 * 对应 dish_ingredient 表，记录每道菜使用了哪些食材及用量。
 * 用于计算菜品的营养成分。
 */
@Data
public class DishIngredient {

    /** 关联ID，主键 */
    private Long id;

    /** 菜品ID，关联 dish 表 */
    private Long dishId;

    /** 食材ID，关联 ingredient 表 */
    private Long ingredientId;

    /** 食材用量，单位：克(g) */
    private BigDecimal weight;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 食材名称，非数据库字段，关联查询时填充 */
    private String ingredientName;

}
