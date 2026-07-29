package com.ws.bitesmart.controller.order;

import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.dto.request.BatchOrderRequest;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.OrderService;
import com.ws.bitesmart.service.order.PaymentService;
import com.ws.bitesmart.mapper.order.OrderStatusLogMapper;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import com.ws.bitesmart.service.payment.AlipayPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOrderControllerTest {

    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private OrderStatusLogMapper orderStatusLogMapper;
    @Mock private RefundApplicationMapper refundApplicationMapper;
    @Mock private AlipayPaymentService alipayPaymentService;

    @Test
    void cancelUsesAuthenticatedUserId() {
        UserOrderController controller = new UserOrderController(orderService, paymentService, orderStatusLogMapper, refundApplicationMapper, alipayPaymentService);

        var result = controller.cancel(new LoginUser(88L, 10), 123L, "不需要了");

        assertThat(result.getCode()).isEqualTo(200);
        verify(orderService).cancelOrder(123L, 88L, "不需要了");
    }

    @Test
    void createPassesAddressAndReceiverDetailsToOrderService() {
        when(orderService.createOrder(88L, "园区一号楼", "张三", "13800000000", "少盐", null, null))
                .thenReturn("ORD-1");

        var result = controller().create(new LoginUser(88L, 10), "园区一号楼", "张三", "13800000000", "少盐", null, null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("orderNo", "ORD-1");
        verify(orderService).createOrder(88L, "园区一号楼", "张三", "13800000000", "少盐", null, null);
    }

    @Test
    void batchCreateSplitsSettlementRemarksByMerchant() {
        BatchOrderRequest request = new BatchOrderRequest();
        request.setAddress("园区一号楼");
        request.setReceiverName("张三");
        request.setReceiverPhone("13800000000");
        BatchOrderRequest.MerchantOrderRequest merchantOrder = new BatchOrderRequest.MerchantOrderRequest();
        merchantOrder.setMerchantId(20L);
        merchantOrder.setRemark("不要香菜");
        request.setMerchantOrders(java.util.List.of(merchantOrder));
        when(orderService.createOrders(org.mockito.ArgumentMatchers.eq(88L), org.mockito.ArgumentMatchers.eq("园区一号楼"), org.mockito.ArgumentMatchers.eq("张三"), org.mockito.ArgumentMatchers.eq("13800000000"), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(java.util.List.of("ORD-2"));

        var result = controller().createBatch(new LoginUser(88L, 10), request);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("orderCount", 1);
        org.mockito.ArgumentCaptor<java.util.Map<Long, String>> captor = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(orderService).createOrders(org.mockito.ArgumentMatchers.eq(88L), org.mockito.ArgumentMatchers.eq("园区一号楼"), org.mockito.ArgumentMatchers.eq("张三"), org.mockito.ArgumentMatchers.eq("13800000000"), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue()).containsEntry(20L, "不要香菜");
    }

    @Test
    void batchCreateRejectsMissingMerchant() {
        BatchOrderRequest request = new BatchOrderRequest();
        BatchOrderRequest.MerchantOrderRequest merchantOrder = new BatchOrderRequest.MerchantOrderRequest();
        request.setMerchantOrders(java.util.List.of(merchantOrder));

        var result = controller().createBatch(new LoginUser(88L, 10), request);

        assertThat(result.getCode()).isEqualTo(400);
    }

    @Test
    void pagedListReturnsFlatPageResponseForTheUserOrderPage() {
        PageInfo<Orders> pageInfo = new PageInfo<>(java.util.List.of(new Orders()));
        when(orderService.getOrdersByUser(88L, 1, 10)).thenReturn(pageInfo);

        Object result = controller().list(new LoginUser(88L, 10), 1, 10);

        assertThat(result).isInstanceOf(PageResultVO.class);
        PageResultVO<?> page = (PageResultVO<?>) result;
        assertThat(page.getData().getList()).hasSize(1);
    }

    private UserOrderController controller() {
        return new UserOrderController(orderService, paymentService, orderStatusLogMapper, refundApplicationMapper, alipayPaymentService);
    }
}
