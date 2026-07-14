package com.ws.bitesmart.service.health;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.health.DietRecord;
import com.ws.bitesmart.entity.health.ExerciseRecord;
import com.ws.bitesmart.entity.health.WeightRecord;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.health.DietRecordMapper;
import com.ws.bitesmart.mapper.health.ExerciseRecordMapper;
import com.ws.bitesmart.mapper.health.WeightRecordMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * 健康记录服务
 *
 * 饮食、运动、体重三种记录都在这一个服务里。
 * 用户端功能：记录每天的健康数据，查看历史记录和趋势。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthRecordService {

    private final DietRecordMapper dietRecordMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final UserProfileMapper userProfileMapper;

    // ==================== 饮食记录 ====================

    /** 查全部饮食记录（按日期倒序） */
    public List<DietRecord> getDietRecords(Long userId) {
        return dietRecordMapper.findByUserId(userId);
    }

    /** 查某一天的饮食记录，按餐次排序 */
    public List<DietRecord> getDietRecordsByDate(Long userId, LocalDate date) {
        return dietRecordMapper.findByUserIdAndDate(userId, date);
    }

    /** 新增饮食记录，默认为手动添加（source_type=20） */
    @Transactional
    public void addDietRecord(Long userId, DietRecord record) {
        record.setId(SnowflakeUtil.generate());
        record.setUserId(userId);
        record.setSourceType(20); // manual records cannot impersonate order imports
        if (record.getRecordDate() == null) record.setRecordDate(LocalDate.now());
        if (record.getRecordTime() == null) record.setRecordTime(LocalTime.now());
        if (record.getQuantity() == null || record.getQuantity() < 1) record.setQuantity(1);
        dietRecordMapper.insert(record);
    }

    /** Import completed order items into diet records. The order item ID makes this idempotent. */
    @Transactional
    public void importOrderDietRecords(Long userId, List<OrderItem> items, LocalDateTime finishedAt) {
        if (items == null || items.isEmpty()) return;
        LocalDateTime recordedAt = finishedAt == null ? LocalDateTime.now() : finishedAt;
        for (OrderItem item : items) {
            if (item.getId() == null || dietRecordMapper.findByOrderItemId(item.getId()) != null) continue;

            int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            DietRecord record = new DietRecord();
            record.setId(SnowflakeUtil.generate());
            record.setUserId(userId);
            record.setRecordDate(recordedAt.toLocalDate());
            record.setRecordTime(recordedAt.toLocalTime());
            record.setMealType(resolveMealType(recordedAt.toLocalTime()));
            record.setFoodName(item.getSnapshotName());
            record.setQuantity(quantity);
            record.setCalories(scale(item.getSnapshotCalories(), quantity));
            record.setProtein(scale(item.getSnapshotProtein(), quantity));
            record.setFat(scale(item.getSnapshotFat(), quantity));
            record.setCarbs(scale(item.getSnapshotCarbs(), quantity));
            record.setSourceType(10);
            record.setOrderItemId(item.getId());
            dietRecordMapper.insert(record);
        }
    }

    private Integer resolveMealType(LocalTime time) {
        int hour = time.getHour();
        if (hour < 10) return 10;
        if (hour < 14) return 20;
        if (hour < 17) return 40;
        return 30;
    }

    private Integer scale(Integer value, int quantity) {
        return value == null ? 0 : value * quantity;
    }

    private BigDecimal scale(BigDecimal value, int quantity) {
        return value == null ? BigDecimal.ZERO : value.multiply(BigDecimal.valueOf(quantity));
    }

    /** 修改饮食记录，校验所属权 */
    @Transactional
    public void updateDietRecord(Long userId, DietRecord record) {
        DietRecord exist = dietRecordMapper.findById(record.getId());
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "记录不存在");
        }
        record.setUserId(userId);
        dietRecordMapper.updateById(record);
    }

    /** 删除饮食记录，校验所属权 */
    @Transactional
    public void deleteDietRecord(Long userId, Long recordId) {
        DietRecord exist = dietRecordMapper.findById(recordId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "记录不存在");
        }
        dietRecordMapper.deleteById(recordId);
    }

    // ==================== 运动记录 ====================

    /** 查全部运动记录 */
    public List<ExerciseRecord> getExerciseRecords(Long userId) {
        return exerciseRecordMapper.findByUserId(userId);
    }

    /** 查某天的运动记录 */
    public List<ExerciseRecord> getExerciseRecordsByDate(Long userId, LocalDate date) {
        return exerciseRecordMapper.findByUserIdAndDate(userId, date);
    }

    /** 新增运动记录 */
    @Transactional
    public void addExerciseRecord(Long userId, ExerciseRecord record) {
        record.setId(SnowflakeUtil.generate());
        record.setUserId(userId);
        exerciseRecordMapper.insert(record);
    }

    /** 删除运动记录，校验所属权 */
    @Transactional
    public void deleteExerciseRecord(Long userId, Long recordId) {
        ExerciseRecord exist = exerciseRecordMapper.findById(recordId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "记录不存在");
        }
        exerciseRecordMapper.deleteById(recordId);
    }

    // ==================== 体重记录 ====================

    /** 查全部体重记录（按日期倒序，前端用来画折线图） */
    public List<WeightRecord> getWeightRecords(Long userId) {
        return weightRecordMapper.findByUserId(userId);
    }

    /** 查全部体重记录（分页） */
    public PageInfo<WeightRecord> getWeightRecords(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<WeightRecord> list = weightRecordMapper.findByUserId(userId);
        return new PageInfo<>(list);
    }

    /**
     * 保存体重记录（有则更新，无则新增）
     * 数据库有 unique(user_id, record_date) 约束，所以同一天只能有一条
     */
    @Transactional
    public void saveWeightRecord(Long userId, WeightRecord record) {
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        record.setUserId(userId);

        WeightRecord exist = weightRecordMapper.findByUserIdAndDate(userId, record.getRecordDate());
        if (exist != null) {
            // 已有当天记录，更新
            record.setId(exist.getId());
            weightRecordMapper.updateByUserIdAndDate(record);
        } else {
            // 没有当天记录，新增
            record.setId(SnowflakeUtil.generate());
            weightRecordMapper.insert(record);
        }
        userProfileMapper.updateWeight(userId, record.getWeight());
    }

}
