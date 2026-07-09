package com.ws.bitesmart.mapper.health;

import com.ws.bitesmart.entity.health.WeightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 体重记录 Mapper
 *
 * 每天一条记录（数据库有 unique 约束），
 * updateByUserIdAndDate 用于同一天已有记录时更新。
 */
@Mapper
public interface WeightRecordMapper {

    /** 查用户的全部体重记录，按日期倒序 */
    List<WeightRecord> findByUserId(@Param("userId") Long userId);

    /** 查某天的记录（用来判断是新增还是更新） */
    WeightRecord findByUserIdAndDate(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate);

    int insert(WeightRecord record);

    int updateByUserIdAndDate(WeightRecord record);

}
