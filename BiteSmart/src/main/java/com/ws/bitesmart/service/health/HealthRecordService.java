package com.ws.bitesmart.service.health;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.health.DietRecord;
import com.ws.bitesmart.entity.health.ExerciseRecord;
import com.ws.bitesmart.entity.health.WeightRecord;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.health.DietRecordMapper;
import com.ws.bitesmart.mapper.health.ExerciseRecordMapper;
import com.ws.bitesmart.mapper.health.WeightRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        if (record.getSourceType() == null) record.setSourceType(20); // 手动添加
        dietRecordMapper.insert(record);
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
    }

}
