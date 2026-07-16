package com.ws.bitesmart.entity.order;

import lombok.Data;

import java.time.LocalDateTime;

/** 持久化记录订单状态的每次变更。 */
@Data
public class OrderStatusLog {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String operatorType;
    private String reason;
    private String source;
    private LocalDateTime createTime;
}
