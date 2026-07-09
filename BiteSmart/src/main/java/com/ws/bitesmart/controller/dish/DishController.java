package com.ws.bitesmart.controller.dish;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.service.dish.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端 - 菜品浏览接口
 *
 * 无需登录即可查看上架菜品。
 * 支持按分类筛选。
 */
@Slf4j
@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /**
     * 查所有上架菜品
     *
     * @param categoryId 可选，按分类筛选
     */
    @GetMapping
    public ResultVO<List<Dish>> list(@RequestParam(required = false) Long categoryId) {
        List<Dish> dishes = dishService.findAvailable();
        if (categoryId != null) {
            dishes = dishes.stream()
                    .filter(d -> categoryId.equals(d.getCategoryId()))
                    .collect(Collectors.toList());
        }
        return ResultVO.success(dishes);
    }

    /** 查菜品详情 */
    @GetMapping("/{id}")
    public ResultVO<Dish> detail(@PathVariable Long id) {
        return ResultVO.success(dishService.findById(id));
    }

}
