package com.ws.bitesmart.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchOrderRequest {
    private String address;
    private String receiverName;
    private String receiverPhone;
    /** 配送方式：10-外卖配送，20-到店自取 */
    private Integer deliveryType;
    /** 收货地址坐标（GCJ-02），外卖配送时使用 */
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<MerchantOrderRequest> merchantOrders;

    @Data
    public static class MerchantOrderRequest {
        private Long merchantId;
        private String remark;
    }
}
