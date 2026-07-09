package com.ws.bitesmart.entity.health;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体重记录表 实体类
 *
 * 对应 weight_record 表。
 * 每天只能记录一条（unique 约束）。
 * BMI 可以自动计算，也可以前端算好传过来。
 */
@Data
public class WeightRecord {

    private Long id;
    private Long userId;

    /** 称重日期，每天唯一 */
    private LocalDate recordDate;

    /** 体重（kg） */
    private BigDecimal weight;

    /** 体脂率（%） */
    private BigDecimal bodyFatRate;

    /** BMI 指数 */
    private BigDecimal bmi;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
