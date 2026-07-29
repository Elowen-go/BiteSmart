package com.ws.bitesmart.dto.request;

import lombok.Data;

/** 骑手端意见反馈请求。 */
@Data
public class DriverFeedbackRequest {
    private Long orderId;
    private String category;
    private String content;
}
