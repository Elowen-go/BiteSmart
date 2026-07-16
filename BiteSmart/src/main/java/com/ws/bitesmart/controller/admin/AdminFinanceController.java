package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.merchant.MerchantFundAccount;
import com.ws.bitesmart.entity.merchant.MerchantSettlement;
import com.ws.bitesmart.mapper.merchant.MerchantFundAccountMapper;
import com.ws.bitesmart.mapper.merchant.MerchantSettlementMapper;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.merchant.MerchantFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceController {
    private final MerchantFundAccountMapper accountMapper;
    private final MerchantSettlementMapper settlementMapper;
    private final MerchantFinanceService financeService;

    @GetMapping("/accounts")
    public ResultVO<List<MerchantFundAccount>> accounts(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(accountMapper.findAll());
    }

    @GetMapping("/settlements")
    public ResultVO<List<MerchantSettlement>> settlements(@AuthenticationPrincipal LoginUser loginUser,
                                                           @RequestParam(required = false) Long merchantId,
                                                           @RequestParam(required = false) Integer status) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(settlementMapper.findAll(merchantId, status));
    }

    @PostMapping("/settlements")
    public ResultVO<MerchantSettlement> createSettlement(@AuthenticationPrincipal LoginUser loginUser,
                                                         @RequestParam Long merchantId,
                                                         @RequestParam BigDecimal amount,
                                                         @RequestParam(required = false) String payoutMethod,
                                                         @RequestParam(required = false) String payoutAccountMask) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(financeService.createSettlement(merchantId, amount, payoutMethod,
                payoutAccountMask, loginUser.getUserId()));
    }

    @PutMapping("/settlements/{id}/approve")
    public ResultVO<Void> approve(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                                  @RequestParam(required = false) String remark) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        financeService.approveSettlement(id, loginUser.getUserId(), remark);
        return ResultVO.ok("Settlement approved");
    }

    @PutMapping("/settlements/{id}/complete")
    public ResultVO<Void> complete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                                   @RequestParam(required = false) String remark) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        financeService.completeSettlement(id, loginUser.getUserId(), remark);
        return ResultVO.ok("Settlement completed");
    }

    @PutMapping("/settlements/{id}/reject")
    public ResultVO<Void> reject(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                                 @RequestParam(required = false) String remark) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        financeService.rejectSettlement(id, loginUser.getUserId(), remark);
        return ResultVO.ok("Settlement rejected");
    }
}
