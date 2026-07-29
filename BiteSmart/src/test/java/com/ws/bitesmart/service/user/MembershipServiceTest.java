package com.ws.bitesmart.service.user;

import com.ws.bitesmart.entity.user.MembershipPaymentOrder;
import com.ws.bitesmart.entity.user.MembershipPlan;
import com.ws.bitesmart.entity.user.UserMembership;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.user.MembershipPaymentOrderMapper;
import com.ws.bitesmart.mapper.user.MembershipPlanMapper;
import com.ws.bitesmart.mapper.user.UserMembershipMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipPlanMapper planMapper;
    @Mock
    private UserMembershipMapper userMembershipMapper;
    @Mock
    private MembershipPaymentOrderMapper paymentOrderMapper;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(planMapper, userMembershipMapper, paymentOrderMapper);
    }

    @Test
    void createPaymentOrderOnlyCreatesPendingPaymentOrder() {
        MembershipPlan plan = activePlan();
        when(planMapper.findById(1L)).thenReturn(plan);

        MembershipPaymentOrder result = service.createPaymentOrder(7L, 1L);

        assertThat(result.getOrderNo()).startsWith(MembershipService.PAYMENT_ORDER_PREFIX);
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getPlanId()).isEqualTo(1L);
        assertThat(result.getPayAmount()).isEqualByComparingTo("19.90");
        assertThat(result.getStatus()).isEqualTo(10);
        verify(paymentOrderMapper).insert(result);
        verifyNoInteractions(userMembershipMapper);
    }

    @Test
    void amountMismatchDoesNotMarkPaymentOrActivateMembership() {
        MembershipPaymentOrder order = pendingOrder();
        when(paymentOrderMapper.findByOrderNo(order.getOrderNo())).thenReturn(order);

        assertThatThrownBy(() -> service.completePayment(
                order.getOrderNo(), "TRADE-1", new BigDecimal("19.89"), LocalDateTime.now()))
                .isInstanceOf(BusinessException.class);

        verify(paymentOrderMapper, never()).markPaidIfPending(any(), any());
        verify(userMembershipMapper, never()).insert(any());
    }

    @Test
    void successfulPaymentMarksOrderAndActivatesMembership() {
        MembershipPaymentOrder order = pendingOrder();
        MembershipPlan plan = activePlan();
        LocalDateTime paidAt = LocalDateTime.of(2026, 7, 29, 23, 0);
        when(paymentOrderMapper.findByOrderNo(order.getOrderNo())).thenReturn(order);
        when(paymentOrderMapper.markPaidIfPending(order.getOrderNo(), "TRADE-1")).thenReturn(1);
        when(planMapper.findById(order.getPlanId())).thenReturn(plan);
        when(userMembershipMapper.findActiveByUserId(order.getUserId())).thenReturn(null);

        service.completePayment(order.getOrderNo(), "TRADE-1", order.getPayAmount(), paidAt);

        ArgumentCaptor<UserMembership> captor = ArgumentCaptor.forClass(UserMembership.class);
        verify(userMembershipMapper).insert(captor.capture());
        UserMembership membership = captor.getValue();
        assertThat(membership.getUserId()).isEqualTo(order.getUserId());
        assertThat(membership.getPlanId()).isEqualTo(order.getPlanId());
        assertThat(membership.getOrderId()).isEqualTo(order.getId());
        assertThat(membership.getStartTime()).isEqualTo(paidAt);
        assertThat(membership.getEndTime()).isEqualTo(paidAt.plusDays(plan.getValidDays()));
    }

    @Test
    void repeatedCallbackDoesNotActivateMembershipTwice() {
        MembershipPaymentOrder order = pendingOrder();
        order.setStatus(20);
        when(paymentOrderMapper.findByOrderNo(order.getOrderNo())).thenReturn(order);

        service.completePayment(order.getOrderNo(), "TRADE-1", new BigDecimal("0.01"), LocalDateTime.now());

        verify(paymentOrderMapper, never()).markPaidIfPending(any(), any());
        verify(userMembershipMapper, never()).insert(any());
    }

    private MembershipPlan activePlan() {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(1L);
        plan.setPlanName("Monthly");
        plan.setPlanType(10);
        plan.setPrice(new BigDecimal("19.90"));
        plan.setValidDays(30);
        plan.setStatus(10);
        return plan;
    }

    private MembershipPaymentOrder pendingOrder() {
        MembershipPaymentOrder order = new MembershipPaymentOrder();
        order.setId(100L);
        order.setOrderNo("MEMBER-100");
        order.setUserId(7L);
        order.setPlanId(1L);
        order.setPayAmount(new BigDecimal("19.90"));
        order.setStatus(10);
        return order;
    }
}
