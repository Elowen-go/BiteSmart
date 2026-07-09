package com.ws.bitesmart.mapper.ai;

import com.ws.bitesmart.entity.ai.NutritionStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

}
