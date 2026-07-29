package com.ws.bitesmart.controller.user;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.user.MembershipPaymentOrder;
import com.ws.bitesmart.entity.user.MembershipPlan;
import com.ws.bitesmart.entity.user.UserMembership;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.payment.AlipayPaymentService;
import com.ws.bitesmart.service.user.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;
    private final AlipayPaymentService alipayPaymentService;

    @GetMapping("/plans")
    public ResultVO<List<MembershipPlan>> getPlans() {
        return ResultVO.success(membershipService.getAvailablePlans());
    }

    @GetMapping("/status")
    public ResultVO<UserMembership> getStatus(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(membershipService.getActiveMembership(loginUser.getUserId()));
    }

    /** Creates an Alipay page form. The callback activates the membership. */
    @PostMapping("/buy/{planId}")
    public ResultVO<Map<String, Object>> buy(@AuthenticationPrincipal LoginUser loginUser,
                                             @PathVariable Long planId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (!alipayPaymentService.isEnabled()) {
            return ResultVO.error(503, "支付宝沙箱未配置，暂不能购买会员");
        }
        MembershipPaymentOrder order = membershipService.createPaymentOrder(loginUser.getUserId(), planId);
        Map<String, Object> result = new HashMap<>();
        result.put("paymentMode", "alipay-sandbox");
        result.put("orderNo", order.getOrderNo());
        result.put("form", alipayPaymentService.createPagePay(order));
        return ResultVO.success("请完成支付宝支付，支付成功后会员自动开通", result);
    }

    @GetMapping("/history")
    public ResultVO<List<UserMembership>> getHistory(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(membershipService.getHistory(loginUser.getUserId()));
    }
}
