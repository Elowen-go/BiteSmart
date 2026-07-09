package com.ws.bitesmart.mapper.delivery;

import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 配送员 Mapper
 *
 * 配送员信息的 CRUD 以及状态、位置更新。
 */
@Mapper
public interface DeliveryDriverMapper {

    /** 按用户ID查配送员信息 */
    DeliveryDriver findByUserId(@Param("userId") Long userId);

    /** 按ID查配送员信息 */
    DeliveryDriver findById(@Param("id") Long id);

    /** 查找可接单配送员（状态=10在线 且 current_orders < max_orders） */
    List<DeliveryDriver> findAvailable();

    /** 新增配送员 */
    int insert(DeliveryDriver driver);

    /** 动态更新配送员（只改非空字段） */
    int updateById(DeliveryDriver driver);

    /** 更新配送员状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 更新配送员坐标 */
    int updateLocation(@Param("id") Long id, @Param("lat") BigDecimal lat, @Param("lng") BigDecimal lng);

    /** 原子增加当前订单数（接单时 +1），返回0表示已达上限 */
    int incrementOrders(@Param("id") Long id);

    /** 原子减少当前订单数（送达时 -1），返回0表示无配送中订单 */
    int decrementOrders(@Param("id") Long id);
}
