package com.ws.bitesmart.entity.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.ws.bitesmart.entity.delivery.DeliveryTask;

/**
 * 订单主表 实体类
 *
 * 对应 orders 表。
 * 订单状态流转：10-待支付 → 20-待接单 → 30-备餐中 → 40-配送中 → 50-已完成
 * 取消/退款：60-已取消 / 70-退款中 / 80-已退款
 */
@Data
public class Orders {

    private Long id;

    /** 订单编号（唯一） */
    private String orderNo;

    private Long userId;
    private Long merchantId;
    private Long deliveryDriverId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付方式：10-支付宝 20-微信 */
    private Integer payMethod;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 订单状态：10-待支付 20-待接单 30-备餐中 40-配送中 50-已完成 60-已取消 70-退款中 80-已退款 */
    private Integer orderStatus;

    /** 配送状态：0-未配送 10-待取餐 20-已取餐 30-配送中 40-已送达 */
    private Integer deliveryStatus;

    /** 配送地址 */
    private String deliveryAddress;

    /** 收货地址坐标（GCJ-02） */
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 订单备注 */
    private String remark;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 关联退款工单 */
    private Long refundId;

    /** 使用的用户优惠券 */
    private Long couponId;

    /** 优惠券抵扣金额 */
    private BigDecimal couponDiscount;

    /** 平台补贴金额 */
    private BigDecimal platformSubsidy;

    /** 锁定库存时间 */
    private LocalDateTime lockStockTime;

    /** 支付超时自动取消时间 */
    private LocalDateTime autoCancelTime;

    /** 下单渠道：PC/小程序/H5 */
    private String channel;

    /** 订单明细（非数据库字段，批量查询时填充） */
    private transient List<OrderItem> items;
    private transient DeliveryTask deliveryTask;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
