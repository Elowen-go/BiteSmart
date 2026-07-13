package com.ws.bitesmart.entity.refund;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundApplication {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundDesc;
    private String evidenceImages;
    private LocalDateTime applyTime;
    private Integer auditStatus;
    private Long auditOperatorId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime refundTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
