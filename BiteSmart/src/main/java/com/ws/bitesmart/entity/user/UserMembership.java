package com.ws.bitesmart.entity.user;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户会员记录表 实体类
 *
 * 对应 user_membership 表。
 * 用户购买会员后生成一条记录，记录有效期。
 * status=10 生效中，20 已过期，30 已退款
 */
@Data
public class UserMembership {

    private Long id;
    private Long userId;
    private Long planId;

    /** 会员类型：10-月卡 20-季卡 30-年卡 */
    private Integer membershipType;

    /** 状态：10-生效中 20-已过期 30-已退款 */
    private Integer status;

    /** 生效开始时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 关联购买订单ID */
    private Long orderId;

    /** 实际支付金额 */
    private BigDecimal payAmount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
