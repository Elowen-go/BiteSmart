package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.PaymentLogMapper;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import com.ws.bitesmart.service.payment.AlipayPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkOrderControllerTest {
    @Mock private RefundApplicationMapper refundMapper;
    @Mock private ComplaintTicketMapper complaintMapper;
    @Mock private OrdersMapper ordersMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private PaymentLogMapper paymentLogMapper;
    @Mock private AlipayPaymentService alipayPaymentService;
    @Mock private MerchantFinanceService merchantFinanceService;

    @Test
    void confirmedRefundSynchronizesOrderToRefunded() {
        RefundApplication refund = new RefundApplication();
        refund.setId(1L);
        refund.setOrderId(200L);
        refund.setAuditStatus(10);
        Orders order = new Orders();
        order.setId(200L);
        when(refundMapper.findById(1L)).thenReturn(refund);
        when(ordersMapper.findById(200L)).thenReturn(order);
        when(refundMapper.updateAudit(1L, 40, null, "approved")).thenReturn(1);
        doNothing().when(alipayPaymentService).refund(order, refund, null);

        ResultVO<Void> result = controller().auditRefund(1L, 40, "approved");

        assertThat(result.getCode()).isEqualTo(200);
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersMapper).updateStatus(captor.capture());
        assertThat(captor.getValue().getOrderStatus()).isEqualTo(80);
        verify(merchantFinanceService).recordRefund(order, refund, null);
    }

    private AdminWorkOrderController controller() {
        return new AdminWorkOrderController(refundMapper, complaintMapper, ordersMapper, sysUserMapper,
                merchantMapper, paymentLogMapper, alipayPaymentService, merchantFinanceService);
    }
}
