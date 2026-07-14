package com.ws.bitesmart.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchOrderRequest {
    private String address;
    private String receiverName;
    private String receiverPhone;
    private List<MerchantOrderRequest> merchantOrders;

    @Data
    public static class MerchantOrderRequest {
        private Long merchantId;
        private String remark;
    }
}
