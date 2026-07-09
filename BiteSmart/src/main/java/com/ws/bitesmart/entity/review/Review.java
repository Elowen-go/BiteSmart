package com.ws.bitesmart.entity.review;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价表 实体类
 *
 * 对应 review 表。
 * 用户对订单、菜品、配送等进行综合评价，商家可回复。
 */
@Data
public class Review {

    /** 主键ID */
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 商家ID */
    private Long merchantId;

    /** 配送员ID */
    private Long driverId;

    /** 菜品ID（可空，空表示对整单评价） */
    private Long dishId;

    /** 菜品评分（1-5） */
    private Integer ratingFood;

    /** 配送评分（1-5） */
    private Integer ratingDelivery;

    /** 服务评分（1-5） */
    private Integer ratingService;

    /** 综合评分（1-5） */
    private BigDecimal overallRating;

    /** 评价内容 */
    private String content;

    /** 评价图片（JSON数组） */
    private String images;

    /** 是否匿名：1-匿名 0-实名 */
    private Integer isAnonymous;

    /** 商家回复内容 */
    private String merchantReply;

    /** 商家回复时间 */
    private LocalDateTime merchantReplyTime;

    /** 状态：10-已发布 20-已隐藏 30-违规删除 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
