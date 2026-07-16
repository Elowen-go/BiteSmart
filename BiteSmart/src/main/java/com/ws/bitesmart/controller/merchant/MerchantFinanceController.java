package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.merchant.MerchantFundAccount;
import com.ws.bitesmart.entity.merchant.MerchantSettlement;
import com.ws.bitesmart.mapper.merchant.MerchantFundLedgerMapper;
import com.ws.bitesmart.mapper.merchant.MerchantSettlementMapper;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/finance")
@RequiredArgsConstructor
public class MerchantFinanceController {
    private final MerchantService merchantService;
    private final MerchantFinanceService financeService;
    private final MerchantFundLedgerMapper ledgerMapper;
    private final MerchantSettlementMapper settlementMapper;

    @GetMapping("/overview")
    public ResultVO<MerchantFundAccount> overview(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = merchantService.getMerchantId(loginUser.getUserId());
        return ResultVO.success(financeService.getOrCreateAccount(merchantId));
    }

    @GetMapping("/ledgers")
    public ResultVO<List<?>> ledgers(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = merchantService.getMerchantId(loginUser.getUserId());
        return ResultVO.success(ledgerMapper.findByMerchantId(merchantId));
    }

    @GetMapping("/settlements")
    public ResultVO<List<MerchantSettlement>> settlements(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = merchantService.getMerchantId(loginUser.getUserId());
        return ResultVO.success(settlementMapper.findByMerchantId(merchantId));
    }
}
