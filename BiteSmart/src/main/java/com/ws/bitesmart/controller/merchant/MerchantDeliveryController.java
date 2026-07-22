package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.delivery.DeliveryTaskService;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 配送管理接口
 *
 * 商家查看自己订单的配送状态和详情。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/delivery")
@RequiredArgsConstructor
public class MerchantDeliveryController {

    private final DeliveryTaskService deliveryTaskService;
    private final MerchantService merchantService;

    /**
     * 商家的配送任务列表（含骑手姓名/电话）
     */
    @GetMapping("/tasks")
    public ResultVO<List<DeliveryTask>> tasks(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        // 商家ID取 merchant.id（不是 userId），与订单/套餐管理接口保持一致
        return ResultVO.success(deliveryTaskService.getMerchantTasks(
                merchantService.getMerchantId(loginUser.getUserId())));
    }

    /**
     * 某个订单的配送详情
     */
    @GetMapping("/tasks/{orderId}")
    public ResultVO<DeliveryTask> detail(@AuthenticationPrincipal LoginUser loginUser,
                                          @PathVariable Long orderId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        DeliveryTask task = deliveryTaskService.getByOrderId(orderId);
        if (task == null) {
            return ResultVO.error(404, "配送任务不存在");
        }
        return ResultVO.success(task);
    }
}
