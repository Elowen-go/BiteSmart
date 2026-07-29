package com.ws.bitesmart.entity.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A pending or paid membership purchase before it becomes a user membership. */
@Data
public class MembershipPaymentOrder {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long planId;
    private BigDecimal payAmount;
    private Integer status;
    private String transactionNo;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
