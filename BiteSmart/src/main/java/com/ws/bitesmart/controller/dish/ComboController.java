package com.ws.bitesmart.controller.dish;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.service.dish.ComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 套餐浏览接口
 *
 * 无需登录即可查看上架套餐。
 * 套餐详情包含关联的菜品列表。
 */
@Slf4j
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    /** 查所有上架套餐 */
    @GetMapping
    public ResultVO<List<Combo>> list() {
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

}
