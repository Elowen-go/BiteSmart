package com.ws.bitesmart.entity.complaint;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintTicket {
    private Long id;
    private Long orderId;
    private Long userId;
    private Integer targetType;
    private Long targetId;
    private String complaintReason;
    private String complaintDesc;
    private String evidenceImages;
    private Integer status;
    private Long adminOperatorId;
    private String adminRemark;
    private String result;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
