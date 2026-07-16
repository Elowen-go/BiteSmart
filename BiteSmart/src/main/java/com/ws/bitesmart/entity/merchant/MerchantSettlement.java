package com.ws.bitesmart.entity.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MerchantSettlement {
    private Long id;
    private Long merchantId;
    private String settlementNo;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal grossAmount;
    private BigDecimal commissionAmount;
    private BigDecimal refundAmount;
    private BigDecimal netAmount;
    private Integer settlementStatus;
    private String payoutMethod;
    private String payoutAccountMask;
    private Long operatorId;
    private String operatorRemark;
    private LocalDateTime paidTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
