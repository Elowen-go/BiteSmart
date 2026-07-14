package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.service.dish.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dishes")
@RequiredArgsConstructor
public class AdminDishController {
    private final DishService dishService;

    @GetMapping
    public ResultVO<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        var pageInfo = dishService.findAllAdmin(page, size);
        pageInfo.getList().forEach(dish -> dish.setDishImage(imageUrl(dish.getDishImage())));
        return ResultVO.success(PageResultVO.success(pageInfo));
    }

    @GetMapping("/{id}")
    public ResultVO<Dish> detail(@PathVariable Long id) { Dish dish = dishService.findById(id); dish.setDishImage(imageUrl(dish.getDishImage())); return ResultVO.success(dish); }

    @PostMapping
    public ResultVO<Void> add(@RequestBody Dish dish) { dishService.add(dish); return ResultVO.ok("新增成功"); }

    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody Dish dish) { dishService.updateByAdmin(id, dish); return ResultVO.ok("更新成功"); }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) { dishService.deleteByAdmin(id); return ResultVO.ok("删除成功"); }

    private String imageUrl(String value) { return value != null && value.startsWith("/uploads/") ? "/api/files/download" + value : value; }
}
