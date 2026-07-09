package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家端 - 订单管理接口
 *
 * 商家查看收到的订单、接单、拒单、备餐、出餐等操作。
 * 商家 ID = 登录用户的 userId（sys_user 中 role_type=20）。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    /** 商家收到的订单列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (page != null) {
            return ResultVO.success(PageResultVO.success(orderService.getOrdersByMerchant(loginUser.getUserId(), page, size)));
        }
        return ResultVO.success(orderService.getOrdersByMerchant(loginUser.getUserId()));
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public ResultVO<Map<String, Object>> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Orders order = orderService.getOrderDetailForMerchant(id, loginUser.getUserId());
        List<OrderItem> items = orderService.getOrderItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return ResultVO.success(result);
    }

    /** 接单（待接单 → 备餐中） */
    @PutMapping("/{id}/accept")
    public ResultVO<Void> accept(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.acceptOrder(id, loginUser.getUserId());
        return ResultVO.ok("已接单");
    }

    /** 拒单（待接单 → 已取消） */
    @PutMapping("/{id}/reject")
    public ResultVO<Void> reject(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestParam String reason) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.rejectOrder(id, loginUser.getUserId(), reason);
        return ResultVO.ok("已拒单");
    }

    /** 开始备餐（待接单 → 备餐中） */
    @PutMapping("/{id}/prepare")
    public ResultVO<Void> prepare(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.prepareOrder(id, loginUser.getUserId());
        return ResultVO.ok("已开始备餐");
    }

    /** 出餐完成 */
    @PutMapping("/{id}/done")
    public ResultVO<Void> done(@AuthenticationPrincipal LoginUser loginUser,
                                @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.finishPreparing(id, loginUser.getUserId());
        return ResultVO.ok("出餐完成");
    }
}
