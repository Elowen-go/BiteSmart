package com.ws.bitesmart.entity.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MerchantFundAccount {
    private Long id;
    private Long merchantId;
    private BigDecimal pendingBalance;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalRefund;
    private BigDecimal totalCommission;
    private Long version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
