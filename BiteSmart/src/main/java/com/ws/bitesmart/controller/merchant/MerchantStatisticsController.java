package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 商家端 - 销售统计接口
 *
 * 提供今日统计、时间段统计、热销菜品排行等数据。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/statistics")
@RequiredArgsConstructor
public class MerchantStatisticsController {

    private final StatisticsService statisticsService;

    /** 今日统计（订单数、销售额） */
    @GetMapping("/today")
    public ResultVO<Map<String, Object>> today(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(statisticsService.getTodayStats(loginUser.getUserId()));
    }

    /** 时间段统计 */
    @GetMapping("/period")
    public ResultVO<Map<String, Object>> period(@AuthenticationPrincipal LoginUser loginUser,
                                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(statisticsService.getPeriodStats(loginUser.getUserId(), startDate, endDate));
    }

    /** 热销菜品排行 */
    @GetMapping("/top-dishes")
    public ResultVO<List<Map<String, Object>>> topDishes(@AuthenticationPrincipal LoginUser loginUser,
                                                          @RequestParam(defaultValue = "10") int limit) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(statisticsService.getTopDishes(loginUser.getUserId(), limit));
    }

    /** 获取时间段内每日统计（用于图表） */
    @GetMapping("/daily")
    public ResultVO<List<Map<String, Object>>> daily(@AuthenticationPrincipal LoginUser loginUser,
                                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(statisticsService.getDailyStats(loginUser.getUserId(), startDate, endDate));
    }

    /** 获取菜品分类销售统计 */
    @GetMapping("/category-revenue")
    public ResultVO<List<Map<String, Object>>> categoryRevenue(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(statisticsService.getCategoryRevenueStats(loginUser.getUserId()));
    }

}
