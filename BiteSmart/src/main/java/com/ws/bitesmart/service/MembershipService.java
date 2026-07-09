package com.ws.bitesmart.service;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.MembershipPlan;
import com.ws.bitesmart.entity.UserMembership;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.MembershipPlanMapper;
import com.ws.bitesmart.mapper.UserMembershipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员服务
 *
 * 用户可以查看可购买的会员套餐、查看自己的会员状态、购买会员。
 * 购买会员后生成一条 user_membership 记录，在有效期内享受权益。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPlanMapper planMapper;
    private final UserMembershipMapper userMembershipMapper;

    // ========== 会员套餐 ==========

    /** 所有上架的会员套餐 */
    public List<MembershipPlan> getAvailablePlans() {
        return planMapper.findAvailable();
    }

    // ========== 用户会员 ==========

    /** 当前生效的会员 */
    public UserMembership getActiveMembership(Long userId) {
        return userMembershipMapper.findActiveByUserId(userId);
    }

    /** 会员购买记录列表 */
    public List<UserMembership> getHistory(Long userId) {
        return userMembershipMapper.findByUserId(userId);
    }

    /**
     * 购买会员
     * 创建一条 user_membership 记录，状态为生效中
     */
    @Transactional
    public UserMembership buyMembership(Long userId, Long planId) {
        MembershipPlan plan = planMapper.findById(planId);
        if (plan == null || plan.getStatus() != 10) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在或已下架");
        }

        // 计算有效期（如果已有生效会员，在原有基础上续期）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now;

        UserMembership active = userMembershipMapper.findActiveByUserId(userId);
        if (active != null && active.getEndTime().isAfter(now)) {
            startTime = active.getEndTime(); // 续期
        }

        UserMembership membership = new UserMembership();
        membership.setId(SnowflakeUtil.generate());
        membership.setUserId(userId);
        membership.setPlanId(planId);
        membership.setMembershipType(plan.getPlanType());
        membership.setStatus(10); // 生效中
        membership.setStartTime(startTime);
        membership.setEndTime(startTime.plusDays(plan.getValidDays()));
        membership.setPayAmount(plan.getPrice());

        userMembershipMapper.insert(membership);
        log.info("会员购买成功: userId={}, planId={}, 有效期至{}", userId, planId, membership.getEndTime());
        return membership;
    }

}
