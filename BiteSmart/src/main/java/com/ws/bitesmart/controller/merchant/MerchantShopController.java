package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家店铺信息接口
 *
 * 已入驻的商家可以查看和修改自己的店铺信息。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/shop")
@RequiredArgsConstructor
public class MerchantShopController {

    private final MerchantService merchantService;

    /** 获取店铺信息 */
    @GetMapping
    public ResultVO<Merchant> getShop(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(merchantService.getByUserId(loginUser.getUserId()));
    }

    /** 修改店铺信息 */
    @PutMapping
    public ResultVO<Void> updateShop(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestBody Merchant merchant) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        merchantService.updateShopInfo(loginUser.getUserId(), merchant);
        return ResultVO.ok("修改成功");
    }

}
