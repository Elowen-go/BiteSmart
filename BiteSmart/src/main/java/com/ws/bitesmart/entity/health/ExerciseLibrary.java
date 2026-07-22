package com.ws.bitesmart.entity.health;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运动库表 实体类
 *
 * 对应 exercise_library 表。
 * 平台预置的常见运动项目，小程序"运动库"页按分类浏览，
 * 选择后按 kcal_per_min × 时长 估算消耗并写入 exercise_record。
 */
@Data
public class ExerciseLibrary {

    private Long id;

    /** 分类：aerobic-有氧 strength-力量 shape-塑形 yoga-瑜伽 */
    private String category;

    /** 运动名称 */
    private String name;

    /** 标准消耗描述，如 "331kcal/5km" */
    private String stdText;

    /** 每分钟消耗热量（大卡） */
    private BigDecimal kcalPerMin;

    /** 默认时长（分钟） */
    private Integer defMins;

    /** 默认距离（公里，无距离概念的运动为 0） */
    private BigDecimal defDist;

    private String imageUrl;

    /** 标签 JSON 数组，如 ["全身","有氧","户外"] */
    private String tags;

    /** 是否常用：0-否 1-是 */
    private Integer hot;

    /** 排序序号，小的在前 */
    private Integer sort;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
