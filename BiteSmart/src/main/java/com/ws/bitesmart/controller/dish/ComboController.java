package com.ws.bitesmart.controller.dish;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.ComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 套餐浏览与换菜接口
 *
 * 无需登录即可查看上架套餐。
 * 套餐详情包含关联的菜品列表。
 * 用户在下单前可以替换套餐中可替换的菜品。
 */
@Slf4j
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    /** 查所有上架套餐 */
    @GetMapping
    public ResultVO<?> list(@RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (page != null) {
            return ResultVO.success(PageResultVO.success(comboService.findAvailable(page, size)));
        }
        return ResultVO.success(comboService.findAvailable());
    }

    /**
     * 查套餐详情（含关联菜品）
     *
     * 返回结构：
     * {
     *   "combo": { ... 套餐信息 ... },
     *   "dishRels": [ ... 关联菜品列表 ... ]
     * }
     */
    @GetMapping("/{id}")
    public ResultVO<Map<String, Object>> detail(@PathVariable Long id) {
        Combo combo = comboService.findById(id);
        List<ComboDishRel> dishRels = comboService.findRelByComboId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("combo", combo);
        result.put("dishRels", dishRels);
        return ResultVO.success(result);
    }

    /**
     * 套餐内菜品更换
     * POST /api/combos/{id}/replace?oldDishId=xxx&newDishId=xxx
     *
     * 换菜前提条件：
     *   1. 套餐设置了 max_replace_count > 0
     *   2. 新菜品在套餐的 replaceable_dish_pool 中
     *   3. 旧菜品在套餐中且 is_fixed = 0
     *
     * 换完后自动重新计算套餐的总热量、蛋白质、脂肪、碳水。
     */
    @PostMapping("/{id}/replace")
    public ResultVO<Map<String, Object>> replace(
            @PathVariable Long id,
            @RequestParam Long oldDishId,
            @RequestParam Long newDishId) {
        Map<String, Object> result = comboService.replaceDish(id, oldDishId, newDishId);
        return ResultVO.success("换菜成功", result);
    }

}
