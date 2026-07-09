package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.DishCategory;
import com.ws.bitesmart.service.dish.DishCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 管理员 - 菜品分类管理
 *
 * 菜品分类是基础数据，管理员可以增删改查所有分类。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final DishCategoryService dishCategoryService;

    @GetMapping
    public ResultVO<List<DishCategory>> list() {
        return ResultVO.success(dishCategoryService.findAll());
    }

    @PostMapping
    public ResultVO<Void> add(@RequestBody DishCategory category) {
        dishCategoryService.add(category);
        return ResultVO.ok("新增成功");
    }

    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody DishCategory category) {
        dishCategoryService.update(id, category);
        return ResultVO.ok("修改成功");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        dishCategoryService.delete(id);
        return ResultVO.ok("删除成功");
    }

}
