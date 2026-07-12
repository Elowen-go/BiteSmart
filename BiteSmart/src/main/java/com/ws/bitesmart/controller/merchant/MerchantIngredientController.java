package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Ingredient;
import com.ws.bitesmart.service.dish.IngredientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 食材查询接口
 *
 * 商家添加菜品时查询可用食材，包括：
 * 1. 查询所有启用的食材
 * 2. 按分类查询食材
 * 3. 查询所有分类名称
 *
 * 注意：商家只能查询，不能修改食材库
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/ingredients")
@RequiredArgsConstructor
public class MerchantIngredientController {

    private final IngredientService ingredientService;

    /**
     * 查询所有启用的食材列表
     * 商家添加菜品时展示所有可用食材
     *
     * @return 启用的食材列表
     */
    @GetMapping
    public ResultVO<List<Ingredient>> list() {
        return ResultVO.success(ingredientService.findAllEnabled());
    }

    /**
     * 按分类查询食材列表
     * 商家可以按分类筛选食材，如只看肉类、只看蔬菜等
     *
     * @param category 分类名称，如：肉类、蔬菜、主食等
     * @return 该分类下的食材列表
     */
    @GetMapping("/by-category")
    public ResultVO<List<Ingredient>> listByCategory(@RequestParam String category) {
        return ResultVO.success(ingredientService.findByCategory(category));
    }

    /**
     * 查询所有分类名称
     * 用于前端展示分类筛选下拉框
     *
     * @return 分类名称列表
     */
    @GetMapping("/categories")
    public ResultVO<List<String>> categories() {
        return ResultVO.success(ingredientService.findAllCategories());
    }

}
