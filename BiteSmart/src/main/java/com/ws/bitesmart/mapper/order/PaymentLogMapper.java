package com.ws.bitesmart.mapper.order;

import com.ws.bitesmart.entity.order.PaymentLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付流水 Mapper
 *
 * 支持支付流水的记录查询和支付成功后的状态更新。
 */
@Mapper
public interface PaymentLogMapper {

    /** 查某订单的支付流水 */
    List<PaymentLog> findByOrderId(@Param("orderId") Long orderId);

    /** 新增支付流水 */
    int insert(PaymentLog paymentLog);

    /** 支付成功后更新状态和流水号 */
    int updatePayStatus(PaymentLog paymentLog);
}
