package com.ws.bitesmart.controller.dish;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.service.dish.DishService;
import com.ws.bitesmart.entity.dish.DishCategory;
import com.ws.bitesmart.service.dish.DishCategoryService;
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
    private final DishCategoryService dishCategoryService;

    /**
     * 查所有上架菜品
     *
     * @param categoryId 可选，按分类筛选
     * @param page       可选，分页页码（传此参数则分页）
     * @param size       每页条数，默认10
     */
    @GetMapping
    public ResultVO<?> list(@RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "latest") String sort,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (page != null) {
            return ResultVO.success(PageResultVO.success(dishService.findAvailableFiltered(keyword, categoryId, sort, page, size)));
        }
        List<Dish> dishes = dishService.findAvailable();
        if (categoryId != null) {
            dishes = dishes.stream()
                    .filter(d -> categoryId.equals(d.getCategoryId()))
                    .collect(Collectors.toList());
        }
        return ResultVO.success(dishes);
    }

    @GetMapping("/categories")
    public ResultVO<List<DishCategory>> categories() {
        return ResultVO.success(dishCategoryService.findAll());
    }

    /** 查菜品详情 */
    @GetMapping("/{id}")
    public ResultVO<Dish> detail(@PathVariable Long id) {
        return ResultVO.success(dishService.findById(id));
    }

}
