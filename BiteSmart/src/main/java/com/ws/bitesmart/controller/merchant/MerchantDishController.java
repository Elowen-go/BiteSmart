package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.DishService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 菜品管理接口
 *
 * 商家管理自己的菜品，数据按 merchantId 隔离。
 * 商家 ID = 登录用户的 userId（sys_user 中 role_type=20 的是商家）。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/dishes")
@RequiredArgsConstructor
public class MerchantDishController {

    private final DishService dishService;

    /** 查自己的菜品列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (page != null) {
            return ResultVO.success(PageResultVO.success(dishService.findByMerchantId(loginUser.getUserId(), page, size)));
        }
        return ResultVO.success(dishService.findByMerchantId(loginUser.getUserId()));
    }

    /** 新增菜品 */
    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestBody Dish dish) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        dish.setMerchantId(loginUser.getUserId());
        dishService.add(dish);
        return ResultVO.ok("新增成功");
    }

    /** 修改菜品 */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestBody Dish dish) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        dishService.update(loginUser.getUserId(), id, dish);
        return ResultVO.ok("修改成功");
    }

    /** 下架菜品 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        dishService.delete(loginUser.getUserId(), id);
        return ResultVO.ok("下架成功");
    }

}
