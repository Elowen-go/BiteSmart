package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.entity.merchant.MerchantAuditLog;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家入驻申请接口
 *
 * 用户注册成为商家后，提交入驻资料等待管理员审核。
 * 审核通过后才能使用商家功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantService merchantService;

    /** 提交入驻申请 */
    @PostMapping("/apply")
    public ResultVO<Void> apply(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody Merchant merchant) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        merchantService.apply(loginUser.getUserId(), merchant);
        return ResultVO.ok("入驻申请已提交，请等待审核");
    }

    /** 查看审核状态 */
    @GetMapping("/status")
    public ResultVO<Merchant> getStatus(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(merchantService.getByUserId(loginUser.getUserId()));
    }

    /** 查看审核记录 */
    @GetMapping("/audit-log")
    public ResultVO<MerchantAuditLog> getAuditLog(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(merchantService.getAuditLog(loginUser.getUserId()));
    }

}
