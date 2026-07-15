package com.ws.bitesmart.controller.order;

import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.OrderService;
import com.ws.bitesmart.service.order.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserOrderControllerTest {

    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;

    @Test
    void cancelUsesAuthenticatedUserId() {
        UserOrderController controller = new UserOrderController(orderService, paymentService);

        var result = controller.cancel(new LoginUser(88L, 10), 123L, "不需要了");

        assertThat(result.getCode()).isEqualTo(200);
        verify(orderService).cancelOrder(123L, 88L, "不需要了");
    }
}
