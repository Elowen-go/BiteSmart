package com.ws.bitesmart.mapper.delivery;

import com.ws.bitesmart.entity.delivery.DriverSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 配送员结算记录 Mapper
 *
 * 结算记录的查询与新增操作。
 */
@Mapper
public interface DriverSettlementMapper {

    /** 按配送员ID查所有结算记录 */
    List<DriverSettlement> findByDriverId(@Param("driverId") Long driverId);

    /** 按配送员ID和结算状态查结算记录 */
    List<DriverSettlement> findByDriverIdAndStatus(@Param("driverId") Long driverId,
                                                   @Param("settlementStatus") Integer settlementStatus);

    /** 管理员查询全部骑手结算记录 */
    List<DriverSettlement> findAll(@Param("settlementStatus") Integer settlementStatus);

    /** 新增结算记录 */
    int insert(DriverSettlement settlement);

    /** 管理员完成内部结算：10-待结算 -> 20-已结算 */
    int transitionStatus(@Param("id") Long id,
                         @Param("expectedStatus") Integer expectedStatus,
                         @Param("newStatus") Integer newStatus);
}
