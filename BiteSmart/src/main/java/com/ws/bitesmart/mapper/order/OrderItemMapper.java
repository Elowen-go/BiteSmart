package com.ws.bitesmart.mapper.order;

import com.ws.bitesmart.entity.order.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细 Mapper
 *
 * 支持按订单ID查询明细，以及批量插入（下单时快照写入）。
 */
@Mapper
public interface OrderItemMapper {

    /** 查某订单的所有明细 */
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    /** 批量插入订单明细 */
    int insertBatch(@Param("list") List<OrderItem> items);
}
