package com.ws.bitesmart.entity.user;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户健康档案表 实体类
 *
 * 对应 user_profile 表，一个用户只有一条档案。
 * diet_preference / allergy_info / disease_history 是 JSON 格式，
 * 用 String 接收，MyBatis 自动处理序列化。
 */
@Data
public class UserProfile {

    private Long id;
    private Long userId;

    /** 年龄 */
    private Integer age;

    /** 性别：10-男 20-女 */
    private Integer gender;

    /** 身高（cm） */
    private BigDecimal height;

    /** 体重（kg） */
    private BigDecimal weight;

    /** 运动量等级：10-久坐 20-轻度 30-中度 40-重度 */
    private Integer activityLevel;

    /** 饮食偏好 JSON，如 ["少盐","少油","素食"] */
    private String dietPreference;

    /** 过敏史 JSON */
    private String allergyInfo;

    /** 疾病史 JSON */
    private String diseaseHistory;

    /** 健康目标：减肥/增肌/维持/控糖/其他 */
    private String healthGoal;

    /** 每日推荐摄入热量（由AI计算） */
    private Integer dailyCalorieTarget;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
