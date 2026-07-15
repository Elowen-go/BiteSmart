package com.ws.bitesmart.service.delivery;

import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.service.health.HealthRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryTaskServiceTest {

    @Mock private DeliveryTaskMapper deliveryTaskMapper;
    @Mock private DeliveryDriverMapper deliveryDriverMapper;
    @Mock private OrdersMapper ordersMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private HealthRecordService healthRecordService;

    @Test
    void acceptTaskUsesDriverIdAndOptimisticTaskUpdate() {
        DeliveryDriver driver = new DeliveryDriver();
        driver.setId(9L);
        driver.setStatus(10);
        driver.setCurrentOrders(0);
        driver.setMaxOrders(5);
        when(deliveryDriverMapper.findByUserId(8L)).thenReturn(driver);
        when(deliveryTaskMapper.acceptTaskWithLock(7L, 9L)).thenReturn(1);

        new DeliveryTaskService(deliveryTaskMapper, deliveryDriverMapper, ordersMapper, orderItemMapper, healthRecordService).acceptTask(7L, 8L);

        verify(deliveryTaskMapper).acceptTaskWithLock(7L, 9L);
        verify(deliveryDriverMapper).incrementOrders(9L);
    }
}
