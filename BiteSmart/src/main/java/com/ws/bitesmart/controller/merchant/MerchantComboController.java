package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.ComboService;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    public PageResultVO<Combo> list(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        Long merchantId = getMerchantId(loginUser, shopId);
        if (page != null) {
            return PageResultVO.success(comboService.findByMerchantId(merchantId, page, size));
        }
        // 不分页时，构造一个 PageResultVO
        List<Combo> list = comboService.findByMerchantId(merchantId);
        return PageResultVO.success(list, list.size(), 1, list.size());
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
        comboService.add(combo, toComboDishRels(request.getDishItems()));
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
        comboService.update(merchantId, id, request.getCombo(), toComboDishRels(request.getDishItems()));
        return ResultVO.ok("修改成功");
    }

    /** DishItem → ComboDishRel 转换 */
    private List<ComboDishRel> toComboDishRels(List<DishItem> items) {
        if (items == null) return null;
        return items.stream()
                .map(d -> {
                    ComboDishRel rel = new ComboDishRel();
                    rel.setDishId(d.getDishId());
                    rel.setQuantity(d.getQuantity() != null ? d.getQuantity() : 1);
                    rel.setIsFixed(d.getIsFixed() != null ? d.getIsFixed() : 1);
                    return rel;
                })
                .toList();
    }

    /** 删除套餐 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        comboService.delete(merchantId, id);
        return ResultVO.ok("删除成功");
    }

    /** 查询套餐详情（含关联菜品） */
    @GetMapping("/{id}")
    public ResultVO<ComboDetailVO> detail(@AuthenticationPrincipal LoginUser loginUser,
                                          @PathVariable Long id,
                                          @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        Combo combo = comboService.findById(id);
        // 校验该套餐是否属于当前商家
        if (!combo.getMerchantId().equals(merchantId)) {
            return ResultVO.error(403, "无权访问该套餐");
        }
        // 查询关联菜品（含 isFixed 信息）
        List<ComboDishRel> rels = comboService.findRelByComboId(id);
        List<DishItem> dishItems = rels.stream()
                .map(r -> new DishItem(r.getDishId(), r.getQuantity(), r.getIsFixed()))
                .toList();
        // 组装VO
        ComboDetailVO vo = new ComboDetailVO();
        vo.setCombo(combo);
        vo.setDishItems(dishItems);
        return ResultVO.success(vo);
    }

    /**
     * 新增/修改套餐的请求体
     */
    @Data
    public static class ComboRequest {
        /** 套餐基本信息 */
        private Combo combo;
        /** 关联的菜品列表（含 isFixed、quantity） */
        private List<DishItem> dishItems;
    }

    /** 套餐关联菜品项 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DishItem {
        private Long dishId;
        private Integer quantity;
        /** 是否固定不可替换：1-固定 0-可替换 */
        private Integer isFixed;
    }

    /**
     * 套餐详情VO
     */
    @Data
    public static class ComboDetailVO {
        private Combo combo;
        private List<DishItem> dishItems;
    }

}
