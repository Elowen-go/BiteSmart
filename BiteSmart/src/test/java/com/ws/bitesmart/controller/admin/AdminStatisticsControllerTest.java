package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.mapper.ai.AiConversationMapper;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.mapper.user.UserMembershipMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsControllerTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private OrdersMapper ordersMapper;
    @Mock private RefundApplicationMapper refundMapper;
    @Mock private ComplaintTicketMapper complaintMapper;
    @Mock private UserMembershipMapper membershipMapper;
    @Mock private AiConversationMapper aiConversationMapper;
    @Mock private DishMapper dishMapper;

    @Test
    void dashboardIncludesOperationalMetrics() {
        when(sysUserMapper.countAll()).thenReturn(100L);
        when(merchantMapper.countAll()).thenReturn(8L);
        when(ordersMapper.countAll()).thenReturn(200L);
        when(ordersMapper.sumPayAmountAll()).thenReturn(BigDecimal.valueOf(5000));
        when(refundMapper.countByStatus(10)).thenReturn(3L);
        when(complaintMapper.countByStatus(10)).thenReturn(4L);
        when(membershipMapper.countActive()).thenReturn(20L);
        when(aiConversationMapper.countAll()).thenReturn(60L);
        when(dishMapper.countLowStock()).thenReturn(5L);
        when(ordersMapper.aggregateDaily(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(ordersMapper.aggregateByStatus()).thenReturn(List.of());

        Map<String, Object> data = new AdminStatisticsController(
                sysUserMapper, merchantMapper, ordersMapper, refundMapper, complaintMapper,
                membershipMapper, aiConversationMapper, dishMapper
        ).dashboard().getData();

        assertThat(data).containsEntry("pendingRefundCount", 3L)
                .containsEntry("pendingComplaintCount", 4L)
                .containsEntry("activeMembershipCount", 20L)
                .containsEntry("aiConversationCount", 60L)
                .containsEntry("lowStockDishCount", 5L);
    }
}
