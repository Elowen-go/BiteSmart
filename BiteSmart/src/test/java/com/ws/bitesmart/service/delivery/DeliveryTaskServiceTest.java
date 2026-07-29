package com.ws.bitesmart.service.delivery;

import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.entity.delivery.DriverSettlement;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
import com.ws.bitesmart.mapper.delivery.DriverSettlementMapper;
import com.ws.bitesmart.mapper.delivery.RiderLocationMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.service.health.HealthRecordService;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DeliveryTaskServiceTest {

    @Mock private DeliveryTaskMapper deliveryTaskMapper;
    @Mock private DeliveryDriverMapper deliveryDriverMapper;
    @Mock private OrdersMapper ordersMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private HealthRecordService healthRecordService;
    @Mock private MerchantFinanceService merchantFinanceService;
    @Mock private MerchantMapper merchantMapper;
    @Mock private RiderLocationMapper riderLocationMapper;
    @Mock private DriverSettlementMapper driverSettlementMapper;

    @Test
    void acceptTaskUsesDriverIdAndOptimisticTaskUpdate() {
        DeliveryDriver driver = new DeliveryDriver();
        driver.setId(9L);
        driver.setStatus(10);
        driver.setCurrentOrders(0);
        driver.setMaxOrders(5);
        when(deliveryDriverMapper.findByUserId(8L)).thenReturn(driver);
        when(deliveryTaskMapper.acceptTaskWithLock(7L, 9L)).thenReturn(1);

        new DeliveryTaskService(deliveryTaskMapper, deliveryDriverMapper, ordersMapper, orderItemMapper,
                merchantMapper, healthRecordService, merchantFinanceService, riderLocationMapper,
                driverSettlementMapper).acceptTask(7L, 8L);

        verify(deliveryTaskMapper).acceptTaskWithLock(7L, 9L);
        verify(deliveryDriverMapper).incrementOrders(9L);
    }

    @Test
    void startDeliveryMovesOwnedPickedUpTaskToInTransit() {
        DeliveryDriver driver = new DeliveryDriver();
        driver.setId(9L);
        DeliveryTask task = new DeliveryTask();
        task.setId(7L);
        task.setDriverId(9L);
        task.setTaskStatus(30);
        when(deliveryTaskMapper.findById(7L)).thenReturn(task);
        when(deliveryDriverMapper.findByUserId(8L)).thenReturn(driver);
        when(deliveryTaskMapper.updateStatusWithLock(7L, 30, 40)).thenReturn(1);

        new DeliveryTaskService(deliveryTaskMapper, deliveryDriverMapper, ordersMapper, orderItemMapper,
                merchantMapper, healthRecordService, merchantFinanceService, riderLocationMapper,
                driverSettlementMapper)
                .startDeliveryTask(7L, 8L);

        verify(deliveryTaskMapper).updateStatusWithLock(7L, 30, 40);
    }

    @Test
    void deliverCreatesOnePendingSettlementRecord() {
        DeliveryDriver driver = new DeliveryDriver();
        driver.setId(9L);
        DeliveryTask task = new DeliveryTask();
        task.setId(7L);
        task.setOrderId(11L);
        task.setDriverId(9L);
        task.setTaskStatus(40);
        when(deliveryTaskMapper.findById(7L)).thenReturn(task);
        when(deliveryDriverMapper.findByUserId(8L)).thenReturn(driver);
        when(deliveryTaskMapper.updateStatusWithLock(7L, 40, 50)).thenReturn(1);

        new DeliveryTaskService(deliveryTaskMapper, deliveryDriverMapper, ordersMapper, orderItemMapper,
                merchantMapper, healthRecordService, merchantFinanceService, riderLocationMapper,
                driverSettlementMapper).deliverTask(7L, 8L);

        verify(driverSettlementMapper).insert(any(DriverSettlement.class));
    }
}
