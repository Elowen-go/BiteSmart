package com.ws.bitesmart.service.order;

import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.review.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售统计服务
 *
 * 基于 orders + order_item 表聚合统计，
 * 提供今日数据、时间段数据、热销菜品排行等能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    /**
     * 获取今日统计（订单数、销售额）
     *
     * @param merchantId 商家ID
     * @return 包含 orderCount、revenue、newUserCount、avgOrderAmount、pendingOrderCount、stockAlertCount、reviewCount 的 Map
     */
    public Map<String, Object> getTodayStats(Long merchantId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        int orderCount = ordersMapper.countByMerchantAndTime(merchantId, start, end);
        BigDecimal revenue = ordersMapper.sumPayAmountByMerchantAndTime(merchantId, start, end);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        BigDecimal avgOrderAmount = BigDecimal.ZERO;
        if (orderCount > 0) {
            avgOrderAmount = revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        }

        int pendingOrderCount = 0;
        int stockAlertCount = 0;
        int reviewCount = 0;
        int newUserCount = 0;

        try {
            reviewCount = reviewMapper.countByMerchantAndTime(merchantId, start, end);
        } catch (Exception e) {
            log.warn("查询今日评价数失败: {}", e.getMessage());
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("orderCount", orderCount);
        stats.put("revenue", revenue);
        stats.put("newUserCount", newUserCount);
        stats.put("avgOrderAmount", avgOrderAmount);
        stats.put("pendingOrderCount", pendingOrderCount);
        stats.put("stockAlertCount", stockAlertCount);
        stats.put("reviewCount", reviewCount);
        return stats;
    }

    /**
     * 获取指定时间段内的统计（订单数、销售额）
     *
     * @param merchantId 商家ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 包含 periodOrders（订单数）和 periodSales（销售额）的 Map
     */
    public Map<String, Object> getPeriodStats(Long merchantId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        int orderCount = ordersMapper.countByMerchantAndTime(merchantId, start, end);
        BigDecimal salesAmount = ordersMapper.sumPayAmountByMerchantAndTime(merchantId, start, end);
        if (salesAmount == null) {
            salesAmount = BigDecimal.ZERO;
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("periodOrders", orderCount);
        stats.put("periodSales", salesAmount);
        return stats;
    }

    /**
     * 获取指定时间段内每日的统计数据（用于图表展示）
     *
     * @param merchantId 商家ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 每日统计列表，每条包含 date、orderCount、revenue
     */
    public List<Map<String, Object>> getDailyStats(Long merchantId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime start = currentDate.atStartOfDay();
            LocalDateTime end = currentDate.atTime(LocalTime.MAX);

            int orderCount = ordersMapper.countByMerchantAndTime(merchantId, start, end);
            BigDecimal revenue = ordersMapper.sumPayAmountByMerchantAndTime(merchantId, start, end);
            if (revenue == null) {
                revenue = BigDecimal.ZERO;
            }

            Map<String, Object> dayStats = new LinkedHashMap<>();
            dayStats.put("date", currentDate.toString());
            dayStats.put("orderCount", orderCount);
            dayStats.put("revenue", revenue);
            dailyStats.add(dayStats);

            currentDate = currentDate.plusDays(1);
        }

        return dailyStats;
    }

    /**
     * 热销菜品排行
     *
     * 基于 order_item 表按菜品 ID 聚合销量，取前 N 名。
     *
     * @param merchantId 商家ID
     * @param limit      返回条数
     * @return 热销菜品排行列表，每条包含 dishId, dishName, totalQuantity
     */
    public List<Map<String, Object>> getTopDishes(Long merchantId, int limit) {
        return orderItemMapper.sumQuantityByMerchant(merchantId, limit);
    }

}
