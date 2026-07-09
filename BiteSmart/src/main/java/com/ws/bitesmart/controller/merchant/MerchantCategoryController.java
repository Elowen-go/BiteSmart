package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.DishCategory;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.DishCategoryService;
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
 * 商家端 - 菜品分类管理接口
 *
 * 菜品分类是商家公用的基础数据，不需要按商家隔离。
 * 分类列表按 sortOrder 排序返回。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/categories")
@RequiredArgsConstructor
public class MerchantCategoryController {

    private final DishCategoryService dishCategoryService;

    /** 查全部分类列表 */
    @GetMapping
    public ResultVO<List<DishCategory>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(dishCategoryService.findAll());
    }

    /** 新增分类 */
    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestBody DishCategory category) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        dishCategoryService.add(category);
        return ResultVO.ok("新增成功");
    }

    /** 修改分类 */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestBody DishCategory category) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        dishCategoryService.update(id, category);
        return ResultVO.ok("修改成功");
    }

    /** 删除分类 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        dishCategoryService.delete(id);
        return ResultVO.ok("删除成功");
    }

}
