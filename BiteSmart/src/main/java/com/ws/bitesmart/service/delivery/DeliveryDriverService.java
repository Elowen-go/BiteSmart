package com.ws.bitesmart.service.delivery;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DriverSettlement;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.mapper.delivery.DriverSettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送员服务
 *
 * 配送员注册、状态管理、位置更新等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryDriverService {

    private final DeliveryDriverMapper deliveryDriverMapper;
    private final DriverSettlementMapper driverSettlementMapper;

    /**
     * 配送员注册
     *
     * @param userId      系统用户ID
     * @param realName    真实姓名
     * @param phone       联系电话
     * @param idCard      身份证号
     * @param vehicleType 车辆类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(Long userId, String realName, String phone, String idCard, Integer vehicleType) {
        // 检查是否已注册
        DeliveryDriver existing = deliveryDriverMapper.findByUserId(userId);
        if (existing != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "该用户已是配送员");
        }

        DeliveryDriver driver = new DeliveryDriver();
        driver.setId(SnowflakeUtil.generate());
        driver.setUserId(userId);
        driver.setRealName(realName);
        driver.setPhone(phone);
        driver.setIdCard(idCard);
        driver.setVehicleType(vehicleType);
        driver.setStatus(30); // 默认离线
        driver.setMaxOrders(5);
        driver.setCurrentOrders(0);
        driver.setAvgRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        deliveryDriverMapper.insert(driver);
        log.info("配送员注册成功: userId={}, driverId={}", userId, driver.getId());
    }

    /**
     * 更新配送员状态
     *
     * @param userId 系统用户ID
     * @param status 10-在线 20-忙碌 30-离线
     */
    public void updateStatus(Long userId, Integer status) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        if (driver.getStatus() == 40) {
            throw new BusinessException("配送员账号已被冻结");
        }
        deliveryDriverMapper.updateStatus(driver.getId(), status);
        log.info("配送员状态更新: driverId={}, status={}", driver.getId(), status);
    }

    /**
     * 更新配送员位置
     *
     * @param userId 系统用户ID
     * @param lat    纬度
     * @param lng    经度
     */
    public void updateLocation(Long userId, BigDecimal lat, BigDecimal lng) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        deliveryDriverMapper.updateLocation(driver.getId(), lat, lng);
        log.debug("配送员位置更新: driverId={}, lat={}, lng={}", driver.getId(), lat, lng);
    }

    /**
     * 获取当前骑手的个人信息
     */
    public DeliveryDriver getProfile(Long userId) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        return driver;
    }

    /**
     * 更新骑手个人信息（只允许基础资料字段：姓名/电话/车辆类型/服务区域）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, DeliveryDriver request) {
        DeliveryDriver driver = getProfile(userId);
        DeliveryDriver update = new DeliveryDriver();
        update.setId(driver.getId());
        update.setRealName(request.getRealName());
        update.setPhone(request.getPhone());
        update.setVehicleType(request.getVehicleType());
        update.setServiceArea(request.getServiceArea());
        deliveryDriverMapper.updateById(update);
        log.info("配送员资料更新: driverId={}", driver.getId());
    }

    /**
     * 查找附近可接单配送员
     *
     * @return 可接单配送员列表
     */
    public List<DeliveryDriver> findAvailableDrivers() {
        return deliveryDriverMapper.findAvailable();
    }

    /**
     * 查询配送员的全部结算记录
     *
     * @param userId 配送员用户ID
     * @return 结算记录列表
     */
    public List<DriverSettlement> getSettlements(Long userId) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        return driverSettlementMapper.findByDriverId(driver.getId());
    }

    /**
     * 查询配送员的收入统计汇总
     *
     * @param userId 配送员用户ID
     * @return 包含总配送费、总奖励、总罚款、总实收、待结算金额、已结算金额的统计Map
     */
    public Map<String, Object> getSettlementStats(Long userId) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        Long driverPk = driver.getId();

        List<DriverSettlement> all = driverSettlementMapper.findByDriverId(driverPk);
        List<DriverSettlement> pending = driverSettlementMapper.findByDriverIdAndStatus(driverPk, 10);
        List<DriverSettlement> settled = driverSettlementMapper.findByDriverIdAndStatus(driverPk, 20);

        BigDecimal totalFee = all.stream()
                .map(s -> s.getDeliveryFee() != null ? s.getDeliveryFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBonus = all.stream()
                .map(s -> s.getBonus() != null ? s.getBonus() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPenalty = all.stream()
                .map(s -> s.getPenalty() != null ? s.getPenalty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = all.stream()
                .map(s -> s.getSettlementAmount() != null ? s.getSettlementAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingAmount = pending.stream()
                .map(s -> s.getSettlementAmount() != null ? s.getSettlementAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settledAmount = settled.stream()
                .map(s -> s.getSettlementAmount() != null ? s.getSettlementAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDeliveryFee", totalFee);
        stats.put("totalBonus", totalBonus);
        stats.put("totalPenalty", totalPenalty);
        stats.put("totalAmount", totalAmount);
        stats.put("pendingAmount", pendingAmount);
        stats.put("settledAmount", settledAmount);
        return stats;
    }

    /** 管理员查询骑手内部结算记录。 */
    public List<DriverSettlement> getAllSettlements(Integer settlementStatus) {
        return driverSettlementMapper.findAll(settlementStatus);
    }

    /** 管理员完成骑手内部结算，暂不触发真实第三方转账。 */
    @Transactional(rollbackFor = Exception.class)
    public void completeSettlement(Long settlementId) {
        if (driverSettlementMapper.transitionStatus(settlementId, 10, 20) != 1) {
            throw new BusinessException("结算记录不存在或已经处理");
        }
    }
}
