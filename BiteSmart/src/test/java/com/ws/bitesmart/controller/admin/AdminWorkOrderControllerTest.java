package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkOrderControllerTest {

    @Mock private RefundApplicationMapper refundMapper;
    @Mock private ComplaintTicketMapper complaintMapper;
    @Mock private OrdersMapper ordersMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private MerchantMapper merchantMapper;

    @Test
    void confirmedRefundSynchronizesOrderToRefunded() {
        RefundApplication refund = new RefundApplication();
        refund.setOrderId(200L);
        Orders order = new Orders();
        order.setId(200L);
        when(refundMapper.updateAudit(1L, 40, null, "同意退款")).thenReturn(1);
        when(refundMapper.findById(1L)).thenReturn(refund);
        when(ordersMapper.findById(200L)).thenReturn(order);

        ResultVO<Void> result = controller().auditRefund(1L, 40, "同意退款");

        assertThat(result.getCode()).isEqualTo(200);
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersMapper).updateStatus(captor.capture());
        assertThat(captor.getValue().getOrderStatus()).isEqualTo(80);
    }

    private AdminWorkOrderController controller() {
        return new AdminWorkOrderController(refundMapper, complaintMapper, ordersMapper, sysUserMapper, merchantMapper);
    }
}
