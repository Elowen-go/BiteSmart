package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.merchant.AdminMerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.pagehelper.PageInfo;

/**
 * 管理员端 - 商家管理（审核）
 *
 * 管理员查看商家列表详情，审核商家入驻申请。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantMapper merchantMapper;
    private final AdminMerchantService adminMerchantService;

    /**
     * 商家列表（分页，可筛选状态）
     * GET /api/admin/merchants?pageNum=1&pageSize=10&status=10
     */
    @GetMapping
    public PageResultVO<Merchant> list(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) Integer status) {
        PageInfo<Merchant> pageInfo = adminMerchantService.findAll(status, pageNum, pageSize);
        return PageResultVO.success(pageInfo);
    }

    /**
     * 商家详情
     * GET /api/admin/merchants/{id}
     */
    @GetMapping("/{id}")
    public ResultVO<Merchant> detail(@PathVariable Long id) {
        Merchant merchant = merchantMapper.findById(id);
        if (merchant == null) {
            return ResultVO.error(404, "商家不存在");
        }
        return ResultVO.success(merchant);
    }

    /**
     * 审核商家入驻
     * PUT /api/admin/merchants/{id}/audit?status=20&auditRemark=审核通过
     * status: 20-审核通过 30-审核驳回
     */
    @PutMapping("/{id}/audit")
    public ResultVO<Void> audit(@PathVariable Long id,
                                @RequestParam Integer status,
                                @RequestParam(required = false) String auditRemark,
                                @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        adminMerchantService.audit(id, status, auditRemark, loginUser.getUserId());
        return ResultVO.ok(status == 20 ? "审核通过" : "审核驳回");
    }

}
