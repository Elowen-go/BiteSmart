package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.OrderService;
import com.ws.bitesmart.service.merchant.MerchantService;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.mapper.order.OrderStatusLogMapper;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
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
 * 商家ID取 merchant.id（通过登录用户 userId 映射），与套餐/菜品管理接口保持一致。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;
    private final SysUserMapper sysUserMapper;
    private final MerchantMapper merchantMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final MerchantService merchantService;
    private final DeliveryTaskMapper deliveryTaskMapper;

    /** 商家ID取 merchant.id（不是 userId），与套餐/菜品管理接口保持一致 */
    private Long getMerchantId(LoginUser loginUser) {
        return merchantService.getMerchantId(loginUser.getUserId());
    }

    /** 商家收到的订单列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser);
        if (page != null) {
            return ResultVO.success(PageResultVO.success(orderService.getOrdersByMerchant(merchantId, page, size)));
        }
        return ResultVO.success(orderService.getOrdersByMerchant(merchantId));
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public ResultVO<Map<String, Object>> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Orders order = orderService.getOrderDetailForMerchant(id, getMerchantId(loginUser));
        List<OrderItem> items = orderService.getOrderItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("buyer", sysUserMapper.findById(order.getUserId()));
        result.put("merchant", merchantMapper.findById(order.getMerchantId()));
        result.put("statusTimeline", orderStatusLogMapper.findByOrderId(id));
        DeliveryTask deliveryTask = deliveryTaskMapper.findByOrderId(id);
        result.put("pickupCode", deliveryTask == null ? null : deliveryTask.getPickupCode());
        return ResultVO.success(result);
    }

    /** 接单（待接单 → 备餐中） */
    @PutMapping("/{id}/accept")
    public ResultVO<Void> accept(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.acceptOrder(id, getMerchantId(loginUser));
        return ResultVO.ok("已接单");
    }

    /** 拒单（待接单 → 已取消） */
    @PutMapping("/{id}/reject")
    public ResultVO<Void> reject(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestParam String reason) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.rejectOrder(id, getMerchantId(loginUser), reason);
        return ResultVO.ok("已拒单");
    }

    /** 开始备餐（待接单 → 备餐中） */
    @PutMapping("/{id}/prepare")
    public ResultVO<Void> prepare(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.prepareOrder(id, getMerchantId(loginUser));
        return ResultVO.ok("已开始备餐");
    }

    /** 出餐完成 */
    @PutMapping("/{id}/done")
    public ResultVO<Void> done(@AuthenticationPrincipal LoginUser loginUser,
                                @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        orderService.finishPreparing(id, getMerchantId(loginUser));
        return ResultVO.ok("出餐完成");
    }
}
