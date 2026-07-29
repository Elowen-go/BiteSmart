package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.user.MembershipPaymentOrder;
import com.ws.bitesmart.entity.user.MembershipPlan;
import com.ws.bitesmart.entity.user.UserMembership;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.user.MembershipPaymentOrderMapper;
import com.ws.bitesmart.mapper.user.MembershipPlanMapper;
import com.ws.bitesmart.mapper.user.UserMembershipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    public static final String PAYMENT_ORDER_PREFIX = "MEMBER-";

    private final MembershipPlanMapper planMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipPaymentOrderMapper paymentOrderMapper;

    public List<MembershipPlan> getAvailablePlans() {
        return planMapper.findAvailable();
    }

    public UserMembership getActiveMembership(Long userId) {
        return userMembershipMapper.findActiveByUserId(userId);
    }

    public List<UserMembership> getHistory(Long userId) {
        return userMembershipMapper.findByUserId(userId);
    }

    /** Creates a pending payment order. Activation happens only after the callback. */
    @Transactional
    public MembershipPaymentOrder createPaymentOrder(Long userId, Long planId) {
        MembershipPlan plan = planMapper.findById(planId);
        if (plan == null || !Integer.valueOf(10).equals(plan.getStatus())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "会员套餐不存在或已下架");
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("会员套餐价格无效");
        }

        MembershipPaymentOrder order = new MembershipPaymentOrder();
        order.setId(SnowflakeUtil.generate());
        order.setOrderNo(PAYMENT_ORDER_PREFIX + order.getId());
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setPayAmount(plan.getPrice());
        order.setStatus(10);
        paymentOrderMapper.insert(order);
        return order;
    }

    /** Activates membership after a verified and amount-matched Alipay callback. */
    @Transactional
    public void completePayment(String orderNo, String transactionNo,
                                BigDecimal paidAmount, LocalDateTime paidAt) {
        MembershipPaymentOrder order = paymentOrderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND, "会员支付订单不存在");
        }
        if (Integer.valueOf(20).equals(order.getStatus())) {
            return;
        }
        if (order.getPayAmount() == null || paidAmount == null
                || order.getPayAmount().compareTo(paidAmount) != 0) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_FAILED, "会员支付金额不一致");
        }

        int affected = paymentOrderMapper.markPaidIfPending(orderNo, transactionNo);
        if (affected == 0) {
            return;
        }

        MembershipPlan plan = planMapper.findById(order.getPlanId());
        if (plan == null || !Integer.valueOf(10).equals(plan.getStatus())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "会员套餐不存在或已下架");
        }

        LocalDateTime now = paidAt == null ? LocalDateTime.now() : paidAt;
        LocalDateTime startTime = now;
        UserMembership active = userMembershipMapper.findActiveByUserId(order.getUserId());
        if (active != null && active.getEndTime() != null && active.getEndTime().isAfter(now)) {
            startTime = active.getEndTime();
        }

        UserMembership membership = new UserMembership();
        membership.setId(SnowflakeUtil.generate());
        membership.setUserId(order.getUserId());
        membership.setPlanId(order.getPlanId());
        membership.setMembershipType(plan.getPlanType());
        membership.setStatus(10);
        membership.setStartTime(startTime);
        membership.setEndTime(startTime.plusDays(plan.getValidDays()));
        membership.setOrderId(order.getId());
        membership.setPayAmount(order.getPayAmount());
        userMembershipMapper.insert(membership);
        log.info("Membership payment completed: orderNo={}, userId={}, planId={}",
                orderNo, order.getUserId(), order.getPlanId());
    }
}
