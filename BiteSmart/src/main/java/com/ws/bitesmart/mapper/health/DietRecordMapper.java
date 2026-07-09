package com.ws.bitesmart.mapper.health;

import com.ws.bitesmart.entity.health.DietRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录 Mapper
 *
 * 支持按日期筛选，按餐次排序（早餐→午餐→晚餐→加餐）
 */
@Mapper
public interface DietRecordMapper {

    /** 查某天某用户的饮食记录 */
    List<DietRecord> findByUserIdAndDate(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate);

    /** 查用户全部饮食记录 */
    List<DietRecord> findByUserId(@Param("userId") Long userId);

    DietRecord findById(@Param("id") Long id);

    int insert(DietRecord record);

    int updateById(DietRecord record);

    int deleteById(@Param("id") Long id);

}
