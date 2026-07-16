package com.ws.bitesmart.service.order;

import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.PaymentLogMapper;
import com.ws.bitesmart.service.system.OperateLogService;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private OrdersMapper ordersMapper;
    @Mock private PaymentLogMapper paymentLogMapper;
    @Mock private DishMapper dishMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ComboDishRelMapper comboDishRelMapper;
    @Mock private OperateLogService operateLogService;
    @Mock private MerchantFinanceService merchantFinanceService;

    @Test
    void payMovesPendingOrderToAcceptedAndWritesPaymentLog() {
        Orders order = new Orders();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setUserId(2L);
        order.setPayAmount(BigDecimal.TEN);
        order.setOrderStatus(10);
        when(ordersMapper.findByOrderNo("ORD-1")).thenReturn(order);
        when(ordersMapper.updateStatusWithLock(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(20), org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(BigDecimal.TEN), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(1);
        when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of());

        new PaymentService(ordersMapper, paymentLogMapper, dishMapper, orderItemMapper, comboDishRelMapper, operateLogService, merchantFinanceService).pay("ORD-1", 10);

        ArgumentCaptor<com.ws.bitesmart.entity.order.PaymentLog> captor = ArgumentCaptor.forClass(com.ws.bitesmart.entity.order.PaymentLog.class);
        verify(paymentLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayStatus()).isEqualTo(20);
        assertThat(captor.getValue().getPayAmount()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
