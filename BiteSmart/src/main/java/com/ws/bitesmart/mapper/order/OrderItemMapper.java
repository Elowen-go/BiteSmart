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

    /** 批量查多个订单的明细（一次性查，防N+1） */
    List<OrderItem> findByOrderIds(@Param("orderIds") List<Long> orderIds);

    /** 批量插入订单明细 */
    int insertBatch(@Param("list") List<OrderItem> items);

    /** 热销菜品排行：按菜品聚合销量，取前 N 名 */
    java.util.List<java.util.Map<String, Object>> sumQuantityByMerchant(@Param("merchantId") Long merchantId,
                                                                         @Param("limit") int limit);

    /** 菜品分类销售统计：按分类聚合销售额 */
    java.util.List<java.util.Map<String, Object>> sumRevenueByCategory(@Param("merchantId") Long merchantId);
}
