package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.MembershipPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会员套餐定义 Mapper
 *
 * 用户端只查已上架(status=10)的套餐。
 * 管理端可以操作全部套餐。
 */
@Mapper
public interface MembershipPlanMapper {

    /** 所有已上架的套餐（按 sort_order 排序） */
    List<MembershipPlan> findAvailable();

    /** 按ID查套餐 */
    MembershipPlan findById(@Param("id") Long id);

}
