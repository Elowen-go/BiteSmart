package com.ws.bitesmart.entity.ai;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营养膳食标准表 实体类
 *
 * 对应 nutrition_standard 表。
 * 按性别、年龄、运动量、目标定义了每日推荐的营养素摄入量。
 * 推荐食谱时根据用户档案匹配最合适的标准。
 */
@Data
public class NutritionStandard {

    private Long id;

    /** 标准名称（如：成年男性减脂标准） */
    private String standardName;

    /** 性别：10-男 20-女 0-不限 */
    private Integer gender;

    /** 最小年龄 */
    private Integer ageMin;

    /** 最大年龄 */
    private Integer ageMax;

    /** 运动量等级：10-久坐 20-轻度 30-中度 40-重度 */
    private Integer activityLevel;

    /** 目标：减肥/增肌/维持 */
    private String targetGoal;

    /** 每日推荐热量（大卡） */
    private Integer dailyCalories;

    /** 每日推荐蛋白质（g） */
    private BigDecimal proteinGrams;

    /** 每日推荐脂肪（g） */
    private BigDecimal fatGrams;

    /** 每日推荐碳水（g） */
    private BigDecimal carbsGrams;

    /** 每日推荐饮水量（ml） */
    private Integer waterMl;

    /** 状态：10-启用 20-停用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
