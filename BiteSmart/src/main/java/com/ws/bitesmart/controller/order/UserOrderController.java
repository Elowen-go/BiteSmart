package com.ws.bitesmart.controller.order;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.OrderService;
import com.ws.bitesmart.service.order.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 订单接口
 *
 * 用户创建订单、查看订单、取消订单、支付订单。
 * 全部从 @AuthenticationPrincipal LoginUser 取 userId。
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * 创建订单
     *
     * @param address       配送地址
     * @param receiverName  收货人姓名
     * @param receiverPhone 收货人电话
     * @param remark        订单备注（可选）
     * @return 包含 orderNo 的响应
     */
    @PostMapping
    public ResultVO<Map<String, String>> create(@AuthenticationPrincipal LoginUser loginUser,
                                                 @RequestParam String address,
                                                 @RequestParam String receiverName,
                                                 @RequestParam String receiverPhone,
                                                 @RequestParam(required = false) String remark) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        String orderNo = orderService.createOrder(loginUser.getUserId(), address,
                receiverName, receiverPhone, remark);
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        return ResultVO.success(result);
    }

    /** 我的订单列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (page != null) {
            return ResultVO.success(PageResultVO.success(orderService.getOrdersByUser(loginUser.getUserId(), page, size)));
        }
        return ResultVO.success(orderService.getOrdersByUser(loginUser.getUserId()));
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public ResultVO<Map<String, Object>> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Orders order = orderService.getOrderDetail(id, loginUser.getUserId());
        List<OrderItem> items = orderService.getOrderItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return ResultVO.success(result);
    }

    /** 取消订单（待支付/待接单状态下可取消） */
    @PostMapping("/{id}/cancel")
    public ResultVO<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestParam(required = false) String reason) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.cancelOrder(id, loginUser.getUserId(), reason);
        return ResultVO.ok("取消成功");
    }

    /**
     * 支付订单
     *
     * @param id        订单ID
     * @param payMethod 支付方式：10-支付宝 20-微信
     */
    @PostMapping("/{id}/pay")
    public ResultVO<Void> pay(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable Long id,
                               @RequestParam Integer payMethod) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        // 先查订单获取 orderNo
        Orders order = orderService.findById(id);
        if (order == null || !order.getUserId().equals(loginUser.getUserId())) {
            return ResultVO.error(404, "订单不存在");
        }
        paymentService.pay(order.getOrderNo(), payMethod);
        return ResultVO.ok("支付成功");
    }
}
