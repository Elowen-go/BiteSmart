package com.ws.bitesmart.service.order;

import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.dto.order.ComboCustomizationSnapshot;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.ShoppingCartMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.service.delivery.DeliveryTaskService;
import com.ws.bitesmart.service.dish.ComboService;
import com.ws.bitesmart.service.system.OperateLogService;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrdersMapper ordersMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private DishMapper dishMapper;
    @Mock private ComboMapper comboMapper;
    @Mock private ComboDishRelMapper comboDishRelMapper;
    @Mock private ComboService comboService;
    @Mock private OperateLogService operateLogService;
    @Mock private DeliveryTaskService deliveryTaskService;
    @Mock private MerchantMapper merchantMapper;
    @Mock private RefundApplicationMapper refundApplicationMapper;

    @Test
    void createOrderReportsSpecificStockShortage() {
        ShoppingCart cart = new ShoppingCart();
        cart.setItemType(10);
        cart.setDishId(7L);
        cart.setQuantity(2);
        Dish dish = new Dish();
        dish.setId(7L);
        dish.setDishName("鸡胸沙拉");
        dish.setPrice(BigDecimal.TEN);
        when(shoppingCartMapper.findSelectedByUserId(88L)).thenReturn(List.of(cart));
        when(dishMapper.findById(7L)).thenReturn(dish);
        when(dishMapper.lockStock(7L, 2)).thenReturn(0);

        assertThatThrownBy(() -> service().createOrder(88L, "地址", "张三", "13800000000", null))
                .hasMessageContaining("鸡胸沙拉")
                .hasMessageContaining("库存不足");
    }

    @Test
    void createComboOrderReportsContainedDishStockShortage() {
        ShoppingCart cart = new ShoppingCart();
        cart.setItemType(20);
        cart.setComboId(9L);
        cart.setQuantity(1);
        Combo combo = new Combo();
        combo.setId(9L);
        combo.setMerchantId(11L);
        combo.setPrice(BigDecimal.valueOf(28));
        combo.setComboName("轻食套餐");
        ComboCustomizationSnapshot snapshot = new ComboCustomizationSnapshot();
        ComboCustomizationSnapshot.SelectedDishItem item = new ComboCustomizationSnapshot.SelectedDishItem();
        item.setDishId(17L);
        item.setDishName("低脂鸡腿");
        item.setQuantity(1);
        snapshot.getItems().add(item);
        when(shoppingCartMapper.findSelectedByUserId(88L)).thenReturn(List.of(cart));
        when(comboMapper.findById(9L)).thenReturn(combo);
        when(comboService.buildCustomizedSnapshot(9L, List.of())).thenReturn(snapshot);
        when(dishMapper.lockStock(17L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service().createOrder(88L, "地址", "张三", "13800000000", null))
                .hasMessageContaining("套餐包含的菜品库存不足");
    }

    @Test
    void pagedUserOrdersIncludeTheirSnapshotItems() {
        Orders order = new Orders();
        order.setId(101L);
        OrderItem item = new OrderItem();
        item.setOrderId(101L);
        item.setSnapshotName("fresh orange juice");
        when(ordersMapper.findByUserId(88L)).thenReturn(List.of(order));
        when(orderItemMapper.findByOrderIds(List.of(101L))).thenReturn(List.of(item));

        PageInfo<Orders> page = service().getOrdersByUser(88L, 1, 10);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getItems()).containsExactly(item);
    }

    private OrderService service() {
        return new OrderService(ordersMapper, orderItemMapper, shoppingCartMapper, dishMapper,
                comboMapper, comboDishRelMapper, comboService, operateLogService, deliveryTaskService,
                merchantMapper, refundApplicationMapper);
    }
}
