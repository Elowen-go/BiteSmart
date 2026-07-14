package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 管理员端 - 数据统计
 *
 * 平台总览数据和趋势统计。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final SysUserMapper sysUserMapper;
    private final MerchantMapper merchantMapper;
    private final OrdersMapper ordersMapper;

    /**
     * 平台总览
     * GET /api/admin/statistics/overview
     *
     * 返回：用户总数、商家总数、订单总数、总营收
     */
    @GetMapping("/overview")
    public ResultVO<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", sysUserMapper.countAll());
        data.put("merchantCount", merchantMapper.countAll());
        data.put("orderCount", ordersMapper.countAll());
        data.put("totalRevenue", ordersMapper.sumPayAmountAll());
        return ResultVO.success(data);
    }

    /**
     * 趋势统计
     * GET /api/admin/statistics/trend?type=day
     * type: day-按日 week-按周 month-按月
     */
    @GetMapping("/trend")
    public ResultVO<Map<String, Object>> trend(@RequestParam(defaultValue = "day") String type) {
        // 根据 type 计算统计时间范围
        LocalDate today = LocalDate.now();
        LocalDateTime startTime;
        switch (type) {
            case "week":
                startTime = today.minusDays(7).atStartOfDay();
                break;
            case "month":
                startTime = today.minusMonths(1).atStartOfDay();
                break;
            default: // day
                startTime = today.minusDays(1).atStartOfDay();
                break;
        }
        LocalDateTime endTime = LocalDateTime.of(today, LocalTime.MAX);

        // 统计数据
        int orderCount = ordersMapper.countByTimeRange(startTime, endTime);
        BigDecimal revenue = ordersMapper.sumPayAmountByTimeRange(startTime, endTime);

        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("orderCount", orderCount);
        data.put("revenue", revenue);
        return ResultVO.success(data);
    }

    @GetMapping("/dashboard")
    public ResultVO<Map<String, Object>> dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overview", overview().getData());
        data.put("dailyTrend", ordersMapper.aggregateDaily(start, end));

        List<Map<String, Object>> statusRows = ordersMapper.aggregateByStatus();
        List<Map<String, Object>> orderStatus = new ArrayList<>();
        for (Map<String, Object> row : statusRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", row.get("order_status"));
            item.put("count", row.get("order_count"));
            orderStatus.add(item);
        }
        data.put("orderStatus", orderStatus);
        return ResultVO.success(data);
    }

}
