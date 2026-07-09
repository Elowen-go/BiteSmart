package com.ws.bitesmart.mapper.delivery;

import com.ws.bitesmart.entity.delivery.DeliveryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 配送任务 Mapper
 *
 * 配送任务的 CRUD 以及状态、位置更新。
 */
@Mapper
public interface DeliveryTaskMapper {

    /** 按订单ID查配送任务 */
    DeliveryTask findByOrderId(@Param("orderId") Long orderId);

    /** 按ID查配送任务 */
    DeliveryTask findById(@Param("id") Long id);

    /** 按配送员ID查任务列表 */
    List<DeliveryTask> findByDriverId(@Param("driverId") Long driverId);

    /** 查询待接单任务（task_status=10） */
    List<DeliveryTask> findPending();

    /** 按商家ID查配送任务列表 */
    List<DeliveryTask> findByMerchantId(@Param("merchantId") Long merchantId);

    /** 新增配送任务 */
    int insert(DeliveryTask task);

    /** 更新配送任务状态（动态更新状态+时间） */
    int updateStatus(DeliveryTask task);

    /** 带乐观锁的配送任务状态更新：仅当当前状态符合预期时才更新 */
    int updateStatusWithLock(@Param("id") Long id,
                             @Param("expectedStatus") Integer expectedStatus,
                             @Param("newStatus") Integer newStatus);

    /** 抢单乐观锁：仅当任务状态=10(待接单)时分配配送员（返回0表示已被抢走） */
    int acceptTaskWithLock(@Param("id") Long id, @Param("driverId") Long driverId);

    /** 更新坐标+路线 */
    int updateLocation(@Param("id") Long id, @Param("lat") BigDecimal lat,
                       @Param("lng") BigDecimal lng, @Param("routeJson") String routeJson);

    /** 上报异常：更新任务状态为60，记录异常原因 */
    int reportException(@Param("id") Long id, @Param("reason") String reason);
}
