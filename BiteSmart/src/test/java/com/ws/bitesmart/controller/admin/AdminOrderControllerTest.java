package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.service.order.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock private OrdersMapper ordersMapper;
    @Mock private DeliveryTaskMapper deliveryTaskMapper;
    @Mock private OrderService orderService;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private MerchantMapper merchantMapper;

    @Test
    void listPassesAllAdminFiltersToTheMapper() {
        when(ordersMapper.findAdminList(12L, 34L, 10, 30, 50)).thenReturn(List.of(new Orders()));
        AdminOrderController controller = new AdminOrderController(
                ordersMapper, deliveryTaskMapper, orderService, orderItemMapper, sysUserMapper, merchantMapper
        );

        PageResultVO<Orders> result = controller.list(1, 10, 50, 12L, 34L, 10, 30);

        assertThat(result.getCode()).isEqualTo(200);
        verify(ordersMapper).findAdminList(12L, 34L, 10, 30, 50);
    }
}
