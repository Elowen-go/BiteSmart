package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.ComboService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 套餐管理接口
 *
 * 商家管理自己的套餐，套餐包含多个菜品（通过 dishIds 关联）。
 * 套餐类型：10-减脂 20-增肌 30-控糖 40-会员专属
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/combos")
@RequiredArgsConstructor
public class MerchantComboController {

    private final ComboService comboService;

    /** 查自己的套餐列表 */
    @GetMapping
    public ResultVO<List<Combo>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(comboService.findByMerchantId(loginUser.getUserId()));
    }

    /** 新增套餐（含关联菜品） */
    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestBody ComboRequest request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Combo combo = request.getCombo();
        combo.setMerchantId(loginUser.getUserId());
        comboService.add(combo, request.getDishIds());
        return ResultVO.ok("新增成功");
    }

    /** 修改套餐 */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestBody ComboRequest request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        comboService.update(loginUser.getUserId(), id, request.getCombo(), request.getDishIds());
        return ResultVO.ok("修改成功");
    }

    /** 下架套餐 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        comboService.delete(loginUser.getUserId(), id);
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
