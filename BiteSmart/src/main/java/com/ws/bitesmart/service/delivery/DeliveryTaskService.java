package com.ws.bitesmart.service.delivery;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.entity.delivery.DriverSettlement;
import com.ws.bitesmart.entity.delivery.RiderLocation;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.service.health.HealthRecordService;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
import com.ws.bitesmart.mapper.delivery.DriverSettlementMapper;
import com.ws.bitesmart.mapper.delivery.RiderLocationMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送任务服务
 *
 * 配送任务的创建、接单、取餐、送达等完整生命周期管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTaskService {

    private final DeliveryTaskMapper deliveryTaskMapper;
    private final DeliveryDriverMapper deliveryDriverMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantMapper merchantMapper;
    private final HealthRecordService healthRecordService;
    private final MerchantFinanceService merchantFinanceService;
    private final RiderLocationMapper riderLocationMapper;
    private final DriverSettlementMapper driverSettlementMapper;

    /** 当前配送费规则，后续可迁移到平台配置。 */
    private static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("5.00");

    /**
     * 创建配送任务（商家出餐后调用）
     *
     * @param order 订单对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTask(Orders order) {
        // 检查是否已存在配送任务
        DeliveryTask existing = deliveryTaskMapper.findByOrderId(order.getId());
        if (existing != null) {
            log.warn("配送任务已存在，跳过创建: orderId={}", order.getId());
            return;
        }

        DeliveryTask task = new DeliveryTask();
        task.setId(SnowflakeUtil.generate());
        task.setOrderId(order.getId());
        task.setOrderNo(order.getOrderNo());
        task.setMerchantId(order.getMerchantId());
        // 快照商家取货点信息（骑手端任务大厅/详情展示、电话联系用）
        Merchant merchant = merchantMapper.findById(order.getMerchantId());
        if (merchant != null) {
            task.setMerchantAddress(merchant.getShopAddress());
            task.setMerchantPhone(merchant.getContactPhone());
            task.setMerchantLat(merchant.getShopLat());
            task.setMerchantLng(merchant.getShopLng());
        }
        task.setDeliveryAddress(order.getDeliveryAddress());
        task.setDeliveryLat(order.getDeliveryLat());
        task.setDeliveryLng(order.getDeliveryLng());
        task.setReceiverName(order.getReceiverName());
        task.setReceiverPhone(order.getReceiverPhone());
        // 快照订单备注（骑手端任务详情展示）
        task.setOrderRemark(order.getRemark());
        // 生成4位随机取餐码
        task.setPickupCode(String.valueOf((int) ((Math.random() * 9000) + 1000)));
        task.setTaskStatus(10); // 待接单
        task.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(30));

        deliveryTaskMapper.insert(task);
        log.info("配送任务创建成功: orderNo={}, taskId={}", order.getOrderNo(), task.getId());
    }

    /**
     * 获取待配送任务列表（待接单任务）
     */
    public List<DeliveryTask> getPendingTasks() {
        return deliveryTaskMapper.findPending();
    }

    /**
     * 抢单（配送员接单）
     *
     * 使用乐观锁防止高并发下多个配送员抢到同一个订单。
     * 数据库层面的原子操作：UPDATE ... WHERE task_status = 10
     * 如果 affectedRows == 0，说明被其他人先抢走了。
     *
     * @param taskId   配送任务ID
     * @param driverId 配送员用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(Long taskId, Long driverId) {
        // 1. 校验配送员状态
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (driver.getStatus() == 40) {
            throw new BusinessException("配送员账号已被冻结");
        }
        if (driver.getCurrentOrders() >= driver.getMaxOrders()) {
            throw new BusinessException("当前配送单数已满，无法接单");
        }

        // 2. 乐观锁抢单：仅当 task_status=10 时才分配配送员
        //    affectedRows=0 表示任务已被其他人抢走
        int affected = deliveryTaskMapper.acceptTaskWithLock(taskId, driver.getId());
        if (affected == 0) {
            throw new BusinessException("订单已被其他配送员抢走");
        }

        // 3. 原子更新配送员当前订单数（乐观锁：current_orders < max_orders 时才 +1）
        deliveryDriverMapper.incrementOrders(driver.getId());

        log.info("配送员抢单成功: taskId={}, driverId={}", taskId, driverId);
    }

    /**
     * 取餐（配送员已取餐）
     *
     * @param taskId   配送任务ID
     * @param driverId 配送员用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void pickupTask(Long taskId, Long driverId) {
        DeliveryTask task = deliveryTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送任务不存在");
        }
        if (!task.getDriverId().equals(deliveryDriverMapper.findByUserId(driverId).getId())) {
            throw new BusinessException("该任务不属于当前配送员");
        }
        if (task.getTaskStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前任务状态不允许取餐操作");
        }

        // 乐观锁更新：仅当当前状态为已接单(20)时才更新为取餐中(30)
        int affected = deliveryTaskMapper.updateStatusWithLock(taskId, 20, 30);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "任务状态已变更，取餐失败");
        }

        // 同步更新订单配送状态：已取餐(20)
        ordersMapper.updateStatusWithLock(
                task.getOrderId(), 40, 40,
                null, null, null, null, null, null, 20);

        log.info("配送员已取餐: taskId={}", taskId);
    }

    /**
     * 开始配送（配送任务 30 已取餐 -> 40 配送中）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void startDeliveryTask(Long taskId, Long driverId) {
        DeliveryTask task = deliveryTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送任务不存在");
        }
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (!task.getDriverId().equals(driver.getId())) {
            throw new BusinessException("该任务不属于当前配送员");
        }
        if (!Integer.valueOf(30).equals(task.getTaskStatus())) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前任务不允许开始配送");
        }

        int affected = deliveryTaskMapper.updateStatusWithLock(taskId, 30, 40);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "任务状态已变更，开始配送失败");
        }
        log.info("配送员开始配送: taskId={}, driverId={}", taskId, driverId);
    }

    /**
     * 送达（配送完成）
     *
     * @param taskId   配送任务ID
     * @param driverId 配送员用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deliverTask(Long taskId, Long driverId) {
        DeliveryTask task = deliveryTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送任务不存在");
        }
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (!task.getDriverId().equals(driver.getId())) {
            throw new BusinessException("该任务不属于当前配送员");
        }
        if (task.getTaskStatus() != 30 && task.getTaskStatus() != 40) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前任务状态不允许送达操作");
        }

        // 乐观锁更新：仅当当前状态为已取餐(30)或配送中(40)时才更新为已送达(50)
        int affected = deliveryTaskMapper.updateStatusWithLock(taskId, task.getTaskStatus(), 50);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "任务状态已变更，送达失败");
        }

        // 原子更新配送员当前订单数
        deliveryDriverMapper.decrementOrders(driver.getId());
        createSettlement(task, driver.getId());
        importCompletedOrderDietRecords(task.getOrderId());

        // 同步更新订单状态：配送中(40) → 已完成(50)，并写入完成时间和配送状态(已送达)
        ordersMapper.updateStatusWithLock(
                task.getOrderId(), 40, 50,
                null, null, null, null, null, LocalDateTime.now(), 40);
        Orders completedOrder = ordersMapper.findById(task.getOrderId());
        if (completedOrder != null) {
            merchantFinanceService.releasePendingIncome(completedOrder);
        }

        log.info("配送完成: taskId={}, driverId={}, orderId={}", taskId, driverId, task.getOrderId());
    }

    /** 送达成功后生成待结算收入记录，状态锁保证同一任务只会生成一次。 */
    private void createSettlement(DeliveryTask task, Long driverId) {
        DriverSettlement settlement = new DriverSettlement();
        settlement.setId(SnowflakeUtil.generate());
        settlement.setDriverId(driverId);
        settlement.setDeliveryTaskId(task.getId());
        settlement.setOrderId(task.getOrderId());
        settlement.setDeliveryFee(DEFAULT_DELIVERY_FEE);
        settlement.setBonus(BigDecimal.ZERO);
        settlement.setPenalty(BigDecimal.ZERO);
        settlement.setSettlementAmount(DEFAULT_DELIVERY_FEE);
        settlement.setSettlementStatus(10);
        driverSettlementMapper.insert(settlement);
    }

    /**
     * 更新配送任务位置和路线
     *
     * @param taskId    配送任务ID
     * @param lat       纬度
     * @param lng       经度
     * @param routeJson 路线JSON
     */
    private void importCompletedOrderDietRecords(Long orderId) {
        Orders completedOrder = ordersMapper.findById(orderId);
        if (completedOrder == null) return;
        healthRecordService.importOrderDietRecords(
                completedOrder.getUserId(), orderItemMapper.findByOrderId(orderId), LocalDateTime.now());
    }

    public void updateLocation(Long taskId, BigDecimal lat, BigDecimal lng, String routeJson) {
        deliveryTaskMapper.updateLocation(taskId, lat, lng, routeJson);
    }

    /**
     * 上报配送异常
     *
     * @param driverId 配送员用户ID
     * @param taskId   配送任务ID
     * @param reason   异常原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportException(Long driverId, Long taskId, String reason) {
        DeliveryTask task = deliveryTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送任务不存在");
        }
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (!task.getDriverId().equals(driver.getId())) {
            throw new BusinessException("该任务不属于当前配送员");
        }
        if (task.getTaskStatus() >= 50) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前任务状态不允许上报异常");
        }

        deliveryTaskMapper.reportException(taskId, reason);
        log.info("配送异常上报: taskId={}, driverId={}, reason={}", taskId, driverId, reason);
    }

    /**
     * 骑手拒单：任务回待接单池（task_status 20→10，driver_id 清空），
     * 记录拒单原因，并回退骑手当前配送单数。
     *
     * @param userId 骑手用户ID
     * @param taskId 配送任务ID
     * @param reason 拒单原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long userId, Long taskId, String reason) {
        DeliveryTask task = deliveryTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送任务不存在");
        }
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (!driver.getId().equals(task.getDriverId())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该任务不属于当前配送员");
        }
        if (!Integer.valueOf(20).equals(task.getTaskStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "只有已接单未取餐的任务才能拒单");
        }

        int affected = deliveryTaskMapper.rejectTask(taskId, driver.getId(), reason);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "任务状态已变更，拒单失败");
        }
        deliveryDriverMapper.decrementOrdersForReject(driver.getId());
        log.info("配送员拒单，任务回待接单池: taskId={}, driverId={}, reason={}", taskId, driver.getId(), reason);
    }

    /**
     * 上传配送轨迹点（坐标系：GCJ-02）。
     * 校验任务属于当前骑手且处于配送途中（30-已取餐 / 40-配送中），
     * 写入 rider_location 并同步任务当前位置。
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadTaskLocation(Long userId, Long taskId, BigDecimal latitude, BigDecimal longitude) {
        DeliveryTask task = deliveryTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送任务不存在");
        }
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (!driver.getId().equals(task.getDriverId())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该任务不属于当前配送员");
        }
        if (!Integer.valueOf(30).equals(task.getTaskStatus()) && !Integer.valueOf(40).equals(task.getTaskStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "只有配送途中的任务才能上传轨迹");
        }

        RiderLocation point = new RiderLocation();
        point.setId(SnowflakeUtil.generate());
        point.setTaskId(taskId);
        point.setDriverId(driver.getId());
        point.setLatitude(latitude);
        point.setLongitude(longitude);
        riderLocationMapper.insert(point);

        // 同步任务当前位置与骑手当前位置
        deliveryTaskMapper.updateLocation(taskId, latitude, longitude, null);
        deliveryDriverMapper.updateLocation(driver.getId(), latitude, longitude);
        log.debug("轨迹点上传: taskId={}, lat={}, lng={}", taskId, latitude, longitude);
    }

    /**
     * 查任务的轨迹点（按上报时间升序）
     */
    public List<RiderLocation> getTaskLocations(Long taskId) {
        return riderLocationMapper.findByTaskId(taskId);
    }

    /**
     * 查询配送员的任务列表
     *
     * @param driverId 配送员用户ID
     */
    public List<DeliveryTask> getDriverTasks(Long driverId) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        return deliveryTaskMapper.findByDriverId(driver.getId());
    }

    /**
     * 查询商家的配送任务列表
     *
     * @param merchantId 商家ID
     */
    public List<DeliveryTask> getMerchantTasks(Long merchantId) {
        return deliveryTaskMapper.findByMerchantId(merchantId);
    }

    /**
     * 按订单ID查询配送详情
     *
     * @param orderId 订单ID
     */
    public DeliveryTask getByOrderId(Long orderId) {
        return deliveryTaskMapper.findByOrderId(orderId);
    }
}
