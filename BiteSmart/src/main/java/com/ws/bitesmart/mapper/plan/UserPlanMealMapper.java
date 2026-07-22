package com.ws.bitesmart.mapper.plan;

import com.ws.bitesmart.entity.plan.UserPlanMeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 计划餐次 Mapper
 */
@Mapper
public interface UserPlanMealMapper {

    int insert(UserPlanMeal meal);

    UserPlanMeal findById(@Param("id") Long id);

    /** 查一份计划的全部餐次，联表带出菜品信息，按天+餐次排序 */
    List<UserPlanMeal> findByPlanId(@Param("planId") Long planId);

    int updateById(UserPlanMeal meal);

}
