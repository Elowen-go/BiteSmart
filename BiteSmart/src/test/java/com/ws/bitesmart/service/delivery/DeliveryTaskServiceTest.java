package com.ws.bitesmart.service.delivery;

import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.entity.delivery.DriverSettlement;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.entity.order.Orders;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void createTaskLeavesEstimatedDeliveryTimeEmptyUntilAccepted() {
        Orders order = new Orders();
        order.setId(11L);
        order.setOrderNo("ORDER-11");
        order.setMerchantId(21L);
        order.setDeliveryType(10);
        order.setDeliveryAddress("测试收货地址");
        order.setDeliveryLat(new BigDecimal("30.2000000"));
        order.setDeliveryLng(new BigDecimal("120.2000000"));
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800138000");

        Merchant merchant = new Merchant();
        merchant.setShopAddress("测试取货地址");
        merchant.setContactPhone("0571-12345678");
        when(deliveryTaskMapper.findByOrderId(11L)).thenReturn(null);
        when(merchantMapper.findById(21L)).thenReturn(merchant);

        new DeliveryTaskService(deliveryTaskMapper, deliveryDriverMapper, ordersMapper, orderItemMapper,
                merchantMapper, healthRecordService, merchantFinanceService, riderLocationMapper,
                driverSettlementMapper).createTask(order);

        ArgumentCaptor<DeliveryTask> captor = ArgumentCaptor.forClass(DeliveryTask.class);
        verify(deliveryTaskMapper).insert(captor.capture());
        assertEquals(10, captor.getValue().getTaskStatus());
        assertNull(captor.getValue().getEstimatedDeliveryTime());
    }

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
