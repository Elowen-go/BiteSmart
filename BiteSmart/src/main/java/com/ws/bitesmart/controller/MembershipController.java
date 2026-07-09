package com.ws.bitesmart.controller;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.MembershipPlan;
import com.ws.bitesmart.entity.UserMembership;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会员中心接口
 *
 * 用户端功能：查看套餐、查看自己会员状态、购买会员。
 * 管理端功能后续在 /api/admin 下开发。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    /** 获取所有可购买的会员套餐 */
    @GetMapping("/plans")
    public ResultVO<List<MembershipPlan>> getPlans() {
        return ResultVO.success(membershipService.getAvailablePlans());
    }

    /** 获取当前用户的会员状态 */
    @GetMapping("/status")
    public ResultVO<UserMembership> getStatus(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        UserMembership active = membershipService.getActiveMembership(loginUser.getUserId());
        return ResultVO.success(active);
    }

    /** 购买会员 */
    @PostMapping("/buy/{planId}")
    public ResultVO<UserMembership> buy(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long planId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        UserMembership membership = membershipService.buyMembership(loginUser.getUserId(), planId);
        return ResultVO.success("购买成功", membership);
    }

    /** 会员购买历史 */
    @GetMapping("/history")
    public ResultVO<List<UserMembership>> getHistory(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(membershipService.getHistory(loginUser.getUserId()));
    }

}
