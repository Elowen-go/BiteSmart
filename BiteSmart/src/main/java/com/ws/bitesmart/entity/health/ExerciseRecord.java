package com.ws.bitesmart.entity.health;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运动记录表 实体类
 *
 * 对应 exercise_record 表。
 * 用户记录每天的运动情况，时长、距离、消耗热量。
 */
@Data
public class ExerciseRecord {

    private Long id;
    private Long userId;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 运动类型（跑步/游泳/健身/瑜伽等） */
    private String exerciseType;

    /** 运动时长（分钟） */
    private Integer duration;

    /** 运动距离（公里） */
    private BigDecimal distance;

    /** 消耗热量（大卡） */
    private Integer caloriesBurned;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
