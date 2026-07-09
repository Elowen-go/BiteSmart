package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜品分类表 实体类
 *
 * 对应 dish_category 表。
 * 支持多级分类（parent_id），目前先按一级分类使用。
 */
@Data
public class DishCategory {

    private Long id;
    private String categoryName;
    private String categoryIcon;
    private Integer sortOrder;

    /** 父级分类ID，0 表示一级分类 */
    private Long parentId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
