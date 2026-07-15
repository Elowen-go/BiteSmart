package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.complaint.ComplaintTicket;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.entity.user.UserMembership;
import com.ws.bitesmart.entity.user.UserProfile;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.mapper.user.UserMembershipMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserMembershipMapper userMembershipMapper;
    @Mock
    private OrdersMapper ordersMapper;
    @Mock
    private ComplaintTicketMapper complaintTicketMapper;

    @Test
    void detailReturnsAccountAndRelatedUserDataWithoutPassword() {
        Long userId = 100L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword("secret");
        UserProfile profile = new UserProfile();
        UserMembership membership = new UserMembership();
        List<Orders> orders = List.of(new Orders());
        List<ComplaintTicket> complaints = List.of(new ComplaintTicket());

        when(sysUserMapper.findById(userId)).thenReturn(user);
        when(userProfileMapper.findByUserId(userId)).thenReturn(profile);
        when(userMembershipMapper.findActiveByUserId(userId)).thenReturn(membership);
        when(ordersMapper.findByUserId(userId)).thenReturn(orders);
        when(complaintTicketMapper.findByUserId(userId)).thenReturn(complaints);

        ResultVO<Map<String, Object>> result = new AdminUserController(
                sysUserMapper, userProfileMapper, userMembershipMapper, ordersMapper, complaintTicketMapper
        ).detail(userId);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(user.getPassword()).isNull();
        assertThat(result.getData())
                .containsEntry("user", user)
                .containsEntry("profile", profile)
                .containsEntry("membership", membership)
                .containsEntry("orders", orders)
                .containsEntry("complaints", complaints);
    }
}
