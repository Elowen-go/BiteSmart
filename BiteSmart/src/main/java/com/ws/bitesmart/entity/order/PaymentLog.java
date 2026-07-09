package com.ws.bitesmart.entity.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水表 实体类
 *
 * 对应 payment_log 表。
 * 记录每笔订单的支付请求和结果。
 * payStatus：10-待支付 20-支付成功 30-支付失败
 */
@Data
public class PaymentLog {

    private Long id;
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 支付方式：10-支付宝 20-微信 */
    private Integer payMethod;

    /** 第三方交易流水号 */
    private String transactionNo;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 支付状态：10-待支付 20-支付成功 30-支付失败 */
    private Integer payStatus;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 支付超时时间 */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
