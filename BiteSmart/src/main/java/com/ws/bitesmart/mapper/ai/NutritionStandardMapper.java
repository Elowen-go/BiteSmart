package com.ws.bitesmart.mapper.ai;

import com.ws.bitesmart.entity.ai.NutritionStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 营养标准 Mapper
 */
@Mapper
public interface NutritionStandardMapper {

    /** 按用户性别/年龄/运动量/目标匹配最合适的标准 */
    NutritionStandard findBestMatch(
            @Param("gender") Integer gender,
            @Param("age") Integer age,
            @Param("activityLevel") Integer activityLevel,
            @Param("targetGoal") String targetGoal);

    /** 查询所有营养标准 */
    List<NutritionStandard> findAll();

    /** 新增营养标准 */
    int insert(NutritionStandard standard);

    /** 更新营养标准 */
    int updateById(NutritionStandard standard);

    /** 逻辑删除营养标准 */
    int deleteById(@Param("id") Long id);

}
