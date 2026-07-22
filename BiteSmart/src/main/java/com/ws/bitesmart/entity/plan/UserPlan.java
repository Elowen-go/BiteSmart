package com.ws.bitesmart.entity.plan;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户专属计划表 实体类
 *
 * 对应 user_plan 表。
 * 用户完成问卷（健康档案）后，系统按档案生成一份 7 天三餐的专属计划。
 * 一个用户可以有多个计划，重新生成时旧计划会被置为已取消。
 */
@Data
public class UserPlan {

    /** 状态：10-未开始 20-进行中 30-已完成 40-已取消 */
    public static final int STATUS_NOT_STARTED = 10;
    public static final int STATUS_RUNNING = 20;
    public static final int STATUS_FINISHED = 30;
    public static final int STATUS_CANCELLED = 40;

    private Long id;
    private Long userId;

    /** 计划天数（当前固定 7 天） */
    private Integer planDays;

    /** 健康目标快照（生成时从 user_profile.health_goal 拷贝） */
    private String goal;

    /** 活动量等级快照：10-久坐 20-轻度 30-中度 40-重度 */
    private Integer activityLevel;

    /** 目标体重（kg） */
    private BigDecimal targetWeight;

    /** 开始体重（kg，生成时档案体重） */
    private BigDecimal startWeight;

    /** 饮食偏好快照 JSON 数组 */
    private String prefs;

    /** 忌口快照 JSON 数组 */
    private String avoid;

    /** 重点部位快照 JSON 数组 */
    private String focusParts;

    /** 状态：10-未开始 20-进行中 30-已完成 40-已取消 */
    private Integer status;

    /** 当前进行到的天（0 起，0 表示第 1 天） */
    private Integer curDay;

    private LocalDateTime createdTime;
    private LocalDateTime startedTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
