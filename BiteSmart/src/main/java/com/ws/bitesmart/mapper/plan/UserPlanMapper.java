package com.ws.bitesmart.mapper.plan;

import com.ws.bitesmart.entity.plan.UserPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 专属计划 Mapper
 */
@Mapper
public interface UserPlanMapper {

    int insert(UserPlan plan);

    UserPlan findById(@Param("id") Long id);

    /** 查用户最新一份计划（按创建时间倒序取第一条） */
    UserPlan findLatestByUserId(@Param("userId") Long userId);

    /** 把用户旧的未开始/进行中计划置为已取消（重新生成时调用） */
    int cancelActiveByUserId(@Param("userId") Long userId);

    int updateById(UserPlan plan);

    /**
     * 定时任务用：所有进行中的计划跨一天。
     * cur_day+1 未超过 plan_days 则 cur_day+1，否则置为已完成（status=30，cur_day 不变）。
     * MySQL SET 从左到右求值，status 先算（用原 cur_day 判断），再更新 cur_day。
     */
    int advanceRunningPlans();

}
