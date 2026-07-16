package com.ws.bitesmart.service.order;

import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.review.ReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private OrdersMapper ordersMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private DishMapper dishMapper;

    @Test
    void todayStatsLoadsAllMerchantDashboardCounters() {
        when(ordersMapper.countByMerchantAndTime(any(), any(), any())).thenReturn(3);
        when(ordersMapper.sumPayAmountByMerchantAndTime(any(), any(), any())).thenReturn(new BigDecimal("28.00"));
        when(ordersMapper.countPendingByMerchantAndTime(any(), any(), any())).thenReturn(2);
        when(ordersMapper.countDistinctUsersByMerchantAndTime(any(), any(), any())).thenReturn(4);
        when(dishMapper.countLowStockByMerchantId(any())).thenReturn(5L);
        when(reviewMapper.countByMerchantAndTime(any(), any(), any())).thenReturn(6);

        var stats = service().getTodayStats(20001L);

        assertThat(stats).containsEntry("orderCount", 3);
        assertThat(stats).containsEntry("revenue", new BigDecimal("28.00"));
        assertThat(stats).containsEntry("pendingOrderCount", 2);
        assertThat(stats).containsEntry("newUserCount", 4);
        assertThat(stats).containsEntry("stockAlertCount", 5);
        assertThat(stats).containsEntry("reviewCount", 6);
    }

    private StatisticsService service() {
        return new StatisticsService(ordersMapper, orderItemMapper, reviewMapper, dishMapper);
    }
}
