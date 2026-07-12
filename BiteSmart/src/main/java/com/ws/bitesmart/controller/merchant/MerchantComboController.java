package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.ComboService;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 套餐管理接口
 *
 * 商家管理自己的套餐，套餐包含多个菜品（通过 dishIds 关联）。
 * 数据按 merchant.id 隔离，支持多店铺。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/combos")
@RequiredArgsConstructor
public class MerchantComboController {

    private final ComboService comboService;
    private final MerchantService merchantService;

    /**
     * 获取当前商家ID（merchant.id）
     * 优先使用请求头中的 X-Shop-Id（多店铺），否则根据登录用户查询
     */
    private Long getMerchantId(LoginUser loginUser, Long shopId) {
        if (shopId != null) {
            return shopId;
        }
        return merchantService.getMerchantId(loginUser.getUserId());
    }

    /** 查自己的套餐列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        if (page != null) {
            return ResultVO.success(PageResultVO.success(comboService.findByMerchantId(merchantId, page, size)));
        }
        return ResultVO.success(comboService.findByMerchantId(merchantId));
    }

    /** 新增套餐（含关联菜品） */
    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestBody ComboRequest request,
                               @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        Combo combo = request.getCombo();
        combo.setMerchantId(merchantId);
        comboService.add(combo, request.getDishIds());
        return ResultVO.ok("新增成功");
    }

    /** 修改套餐 */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestBody ComboRequest request,
                                  @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        comboService.update(merchantId, id, request.getCombo(), request.getDishIds());
        return ResultVO.ok("修改成功");
    }

    /** 下架套餐 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        comboService.delete(merchantId, id);
        return ResultVO.ok("下架成功");
    }

    /**
     * 新增/修改套餐的请求体
     */
    @Data
    public static class ComboRequest {
        /** 套餐基本信息 */
        private Combo combo;
        /** 关联的菜品 ID 列表 */
        private List<Long> dishIds;
    }

}
