package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.service.merchant.MerchantService;
import com.ws.bitesmart.service.order.StatisticsService;
import com.ws.bitesmart.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantStatisticsControllerTest {

    @Mock private StatisticsService statisticsService;
    @Mock private MerchantService merchantService;

    @Test
    void todayStatsUsesMerchantIdInsteadOfLoginUserId() {
        when(merchantService.getMerchantId(88L)).thenReturn(20001L);
        when(statisticsService.getTodayStats(20001L)).thenReturn(Map.of("revenue", BigDecimal.TEN));

        MerchantStatisticsController controller = new MerchantStatisticsController(statisticsService, merchantService);
        var result = controller.today(new LoginUser(88L, 20));

        assertThat(result.getCode()).isEqualTo(200);
        verify(merchantService).getMerchantId(88L);
        verify(statisticsService).getTodayStats(20001L);
    }
}
