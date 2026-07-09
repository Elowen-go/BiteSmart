package com.ws.bitesmart.mapper.health;

import com.ws.bitesmart.entity.health.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 运动记录 Mapper
 */
@Mapper
public interface ExerciseRecordMapper {

    List<ExerciseRecord> findByUserIdAndDate(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate);

    List<ExerciseRecord> findByUserId(@Param("userId") Long userId);

    ExerciseRecord findById(@Param("id") Long id);

    int insert(ExerciseRecord record);

    int deleteById(@Param("id") Long id);

}
