package com.ws.bitesmart.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 管理员端 - 订单管理
 *
 * 管理员查看所有用户订单列表和详情。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrdersMapper ordersMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final OrderService orderService;
    private final OrderItemMapper orderItemMapper;
    private final SysUserMapper sysUserMapper;
    private final MerchantMapper merchantMapper;

    /**
     * 所有用户订单（分页）
     * GET /api/admin/orders?pageNum=1&pageSize=10
     */
    @GetMapping
    public PageResultVO<Orders> list(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) Integer orderStatus) {
        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = orderStatus == null
                ? ordersMapper.findAll()
                : ordersMapper.findAllByStatus(orderStatus);
        return PageResultVO.success(new PageInfo<>(list));
    }

    /**
     * 订单详情
     * GET /api/admin/orders/{id}
     */
    @GetMapping("/{id}")
    public ResultVO<Map<String, Object>> detail(@PathVariable Long id) {
        Orders order = ordersMapper.findById(id);
        if (order == null) {
            return ResultVO.error(404, "订单不存在");
        }
        order.setDeliveryTask(deliveryTaskMapper.findByOrderId(id));
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", orderItemMapper.findByOrderId(id));
        result.put("buyer", sysUserMapper.findById(order.getUserId()));
        result.put("merchant", merchantMapper.findById(order.getMerchantId()));
        return ResultVO.success(result);
    }

    @PutMapping("/{id}/cancel")
    public ResultVO<Void> cancel(@PathVariable Long id,
                                 @RequestParam(required = false) String reason) {
        Orders order = ordersMapper.findById(id);
        if (order == null) return ResultVO.error(404, "订单不存在");
        orderService.cancelOrder(id, order.getUserId(), reason == null ? "管理员取消" : reason);
        return ResultVO.ok("订单已取消");
    }

    @GetMapping("/delivery-tasks")
    public PageResultVO<com.ws.bitesmart.entity.delivery.DeliveryTask> deliveryTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer taskStatus) {
        PageHelper.startPage(pageNum, pageSize);
        return PageResultVO.success(new PageInfo<>(deliveryTaskMapper.findAllAdmin(taskStatus)));
    }

    @GetMapping("/delivery-tasks")
    public PageResultVO<com.ws.bitesmart.entity.delivery.DeliveryTask> deliveryTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer taskStatus) {
        PageHelper.startPage(pageNum, pageSize);
        return PageResultVO.success(new PageInfo<>(deliveryTaskMapper.findAllAdmin(taskStatus)));
    }

}
