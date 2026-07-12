package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Ingredient;
import com.ws.bitesmart.service.dish.IngredientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端 - 食材管理接口
 *
 * 管理员维护平台食材库，包括：
 * 1. 查看食材列表（分页）
 * 2. 新增食材
 * 3. 编辑食材
 * 4. 删除食材
 * 5. 查看所有分类
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ingredients")
@RequiredArgsConstructor
public class AdminIngredientController {

    private final IngredientService ingredientService;

    /**
     * 查询食材列表（分页）
     * 管理员查看所有启用的食材
     *
     * @param page 页码，从1开始，默认1
     * @param size 每页条数，默认10
     * @return 分页食材列表
     */
    @GetMapping
    public ResultVO<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResultVO.success(PageResultVO.success(ingredientService.findAllEnabled(page, size)));
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

    /**
     * 根据ID查询食材详情
     * 用于编辑食材时回显数据
     *
     * @param id 食材ID
     * @return 食材详情
     */
    @GetMapping("/{id}")
    public ResultVO<Ingredient> detail(@PathVariable Long id) {
        Ingredient ingredient = ingredientService.findById(id);
        if (ingredient == null) {
            return ResultVO.error(404, "食材不存在");
        }
        return ResultVO.success(ingredient);
    }

    /**
     * 新增食材
     * 管理员添加新食材到食材库
     *
     * @param ingredient 食材信息
     * @return 操作结果
     */
    @PostMapping
    public ResultVO<Void> add(@RequestBody Ingredient ingredient) {
        ingredientService.add(ingredient);
        return ResultVO.ok("新增成功");
    }

    /**
     * 更新食材
     * 管理员修改食材信息
     *
     * @param id         食材ID
     * @param ingredient 食材信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody Ingredient ingredient) {
        ingredient.setId(id);
        ingredientService.update(ingredient);
        return ResultVO.ok("更新成功");
    }

    /**
     * 删除食材
     * 逻辑删除，不是物理删除
     *
     * @param id 食材ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        ingredientService.delete(id);
        return ResultVO.ok("删除成功");
    }

}
