package com.ws.bitesmart.mapper.order;

import com.ws.bitesmart.entity.order.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper
 *
 * 支持用户端和商家端的订单查询，以及订单状态流转更新。
 */
@Mapper
public interface OrdersMapper {

    /** 按订单号查 */
    Orders findByOrderNo(@Param("orderNo") String orderNo);

    /** 按ID查 */
    Orders findById(@Param("id") Long id);

    /** 用户查自己的订单，按时间倒序 */
    List<Orders> findByUserId(@Param("userId") Long userId);

    /** 商家查收到的订单，按时间倒序 */
    List<Orders> findByMerchantId(@Param("merchantId") Long merchantId);

    /** 新增订单 */
    int insert(Orders orders);

    /** 更新订单状态（支持部分字段更新） */
    int updateStatus(Orders orders);

    /** 带乐观锁的订单状态更新：仅当当前状态符合预期时才更新 */
    int updateStatusWithLock(@Param("id") Long id,
                             @Param("expectedStatus") Integer expectedStatus,
                             @Param("orderStatus") Integer orderStatus,
                             @Param("payMethod") Integer payMethod,
                             @Param("payAmount") java.math.BigDecimal payAmount,
                             @Param("payTime") java.time.LocalDateTime payTime,
                             @Param("cancelTime") java.time.LocalDateTime cancelTime,
                             @Param("cancelReason") String cancelReason,
                             @Param("finishTime") java.time.LocalDateTime finishTime,
                             @Param("deliveryStatus") Integer deliveryStatus);

    /** 统计某商家在指定时间范围内的已完成订单数 */
    int countByMerchantAndTime(@Param("merchantId") Long merchantId,
                               @Param("start") java.time.LocalDateTime start,
                               @Param("end") java.time.LocalDateTime end);

    /** 统计某商家在指定时间范围内的已完成订单销售额 */
    java.math.BigDecimal sumPayAmountByMerchantAndTime(@Param("merchantId") Long merchantId,
                                                       @Param("start") java.time.LocalDateTime start,
                                                       @Param("end") java.time.LocalDateTime end);
}
