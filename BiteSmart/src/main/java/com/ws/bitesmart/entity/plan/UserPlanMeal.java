package com.ws.bitesmart.entity.plan;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计划餐次表 实体类
 *
 * 对应 user_plan_meal 表，一份计划 7 天 × 3 餐 = 21 条。
 * 查询时联表带出菜品信息（dishName 等非表字段），方便小程序直接渲染。
 */
@Data
public class UserPlanMeal {

    private Long id;
    private Long planId;

    /** 第几天（0 起，与 user_plan.cur_day 对齐） */
    private Integer dayIndex;

    /** 餐次：0-早餐 1-午餐 2-晚餐 */
    private Integer mealIndex;

    private Long dishId;

    /** 换菜次数（用于菜品池偏移重新选菜） */
    private Integer swapCount;

    /** 是否已打卡：0-未打卡 1-已打卡 */
    private Integer checked;

    private LocalDateTime checkedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 非表字段：置 true 时把 checked_time 清空（取消打卡用） */
    private Boolean clearCheckedTime;

    // ==================== 联表带出的菜品信息（非表字段） ====================

    private String dishName;
    private String dishImage;

    /** 热量（大卡） */
    private Integer calories;

    /** 蛋白质（g） */
    private BigDecimal protein;

    /** 脂肪（g） */
    private BigDecimal fat;

    /** 碳水（g） */
    private BigDecimal carbs;

    /** 菜品标签 JSON 数组 */
    private String tags;

}
