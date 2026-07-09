package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 套餐关联菜品关系表 实体类
 *
 * 对应 combo_dish_rel 表。
 * 一个套餐包含多个菜品，每个菜品有份数和是否可替换标记。
 */
@Data
public class ComboDishRel {

    private Long id;
    private Long comboId;
    private Long dishId;

    /** 该菜品在套餐中的份数 */
    private Integer quantity;

    /** 是否固定不可替换：1-固定 0-可替换 */
    private Integer isFixed;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
