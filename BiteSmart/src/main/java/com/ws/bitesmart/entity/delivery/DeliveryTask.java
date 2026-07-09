package com.ws.bitesmart.entity.delivery;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送任务表 实体类
 *
 * 对应 delivery_task 表。
 * 商家出餐完成后创建配送任务，由配送员接单并完成配送。
 */
@Data
public class DeliveryTask {

    /** 主键ID */
    private Long id;

    /** 订单ID（唯一） */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 配送员ID */
    private Long driverId;

    /** 商家ID */
    private Long merchantId;

    /** 商家地址 */
    private String merchantAddress;

    /** 商家联系电话 */
    private String merchantPhone;

    /** 配送地址 */
    private String deliveryAddress;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 取餐码 */
    private String pickupCode;

    /** 任务状态：10-待接单 20-待取餐 30-已取餐 40-配送中 50-已送达 60-异常 70-已取消 */
    private Integer taskStatus;

    /** 当前纬度 */
    private BigDecimal currentLat;

    /** 当前经度 */
    private BigDecimal currentLng;

    /** 坐标更新时间 */
    private LocalDateTime locationUpdateTime;

    /** 取餐时间 */
    private LocalDateTime pickupTime;

    /** 送达时间 */
    private LocalDateTime deliverTime;

    /** 预计送达时间 */
    private LocalDateTime estimatedDeliveryTime;

    /** 配送路线（JSON） */
    private String routeJson;

    /** 异常原因 */
    private String exceptionReason;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
