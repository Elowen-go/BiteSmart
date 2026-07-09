package com.ws.bitesmart.mapper;

import com.ws.bitesmart.entity.MembershipPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MembershipPlanMapper {

    /** 所有已上架的套餐 */
    List<MembershipPlan> findAvailable();

    MembershipPlan findById(@Param("id") Long id);

}
