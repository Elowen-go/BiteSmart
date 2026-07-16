package com.ws.bitesmart.entity.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MerchantFundLedger {
    private Long id;
    private Long merchantId;
    private Long orderId;
    private Long paymentLogId;
    private Long refundId;
    private Long settlementId;
    private Integer ledgerType;
    private Integer direction;
    private Integer balanceScope;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String idempotencyKey;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
