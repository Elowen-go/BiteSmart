package com.ws.bitesmart.mapper.order;

import com.ws.bitesmart.entity.order.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderStatusLogMapper {
    List<OrderStatusLog> findByOrderId(@Param("orderId") Long orderId);
}
