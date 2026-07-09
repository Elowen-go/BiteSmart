package com.ws.bitesmart.entity.ai;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI推荐规则配置表 实体类
 *
 * 对应 ai_recommend_rule 表。
 * 管理员配置不同目标人群的推荐规则，AI 根据规则生成食谱建议。
 * status=10 启用，20 停用。
 */
@Data
public class AiRecommendRule {

    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 目标人群：减肥/增肌/控糖/维持 */
    private String targetGoal;

    /** Prompt模板，含变量占位符 */
    private String promptTemplate;

    /** 热量浮动百分比（±%） */
    private Integer calorieFloat;

    /** 最高热量限制 */
    private Integer maxCalories;

    /** 最低热量限制 */
    private Integer minCalories;

    /** 蛋白质供能比（%） */
    private BigDecimal proteinRatio;

    /** 脂肪供能比（%） */
    private BigDecimal fatRatio;

    /** 碳水供能比（%） */
    private BigDecimal carbsRatio;

    /** 模型版本号 */
    private String modelVersion;

    /** 状态：10-启用 20-停用 */
    private Integer status;

    /** 优先级 */
    private Integer priority;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
