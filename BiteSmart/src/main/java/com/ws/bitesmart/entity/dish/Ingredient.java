package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食材实体类
 *
 * 对应 ingredient 表，存储平台预设的食材及营养成分信息。
 * 商家添加菜品时可以选择关联食材，系统自动计算菜品营养成分。
 */
@Data
public class Ingredient {

    /** 食材ID，主键，使用雪花算法生成 */
    private Long id;

    /** 食材名称，如：鸡胸肉、西兰花、大米等 */
    private String name;

    /** 食材分类ID，预留字段，后续可扩展分类管理功能 */
    private Long categoryId;

    /** 食材分类名称，如：肉类、蔬菜、主食、海鲜等 */
    private String categoryName;

    /** 每100克食材的热量，单位：大卡（kcal） */
    private BigDecimal calories;

    /** 每100克食材的蛋白质含量，单位：克（g） */
    private BigDecimal protein;

    /** 每100克食材的脂肪含量，单位：克（g） */
    private BigDecimal fat;

    /** 每100克食材的碳水化合物含量，单位：克（g） */
    private BigDecimal carbs;

    /**
     * 食材状态
     * 10 - 启用（正常可用）
     * 20 - 禁用（暂时不可用）
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 是否删除标记
     * 0 - 未删除
     * 1 - 已删除（逻辑删除）
     */
    private Integer deleted;

}
