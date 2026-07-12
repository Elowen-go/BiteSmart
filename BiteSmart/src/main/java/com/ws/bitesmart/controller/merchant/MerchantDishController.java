package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.DishService;
import com.ws.bitesmart.service.merchant.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 菜品管理接口
 *
 * 商家管理自己的菜品，数据按 merchantId（merchant.id）隔离。
 * 支持多店铺，通过当前登录用户关联到具体店铺的 merchant.id。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/dishes")
@RequiredArgsConstructor
public class MerchantDishController {

    private final DishService dishService;
    private final MerchantService merchantService;

    /**
     * 获取当前商家ID（merchant.id）
     * 优先使用请求头中的 X-Shop-Id（多店铺），否则根据登录用户查询
     */
    private Long getMerchantId(LoginUser loginUser, Long shopId) {
        if (shopId != null) {
            return shopId;
        }
        return merchantService.getMerchantId(loginUser.getUserId());
    }

    /** 查自己的菜品列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        if (page != null) {
            return ResultVO.success(PageResultVO.success(dishService.findByMerchantId(merchantId, page, size)));
        }
        return ResultVO.success(dishService.findByMerchantId(merchantId));
    }

    /** 新增菜品 */
    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestBody Dish dish,
                               @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        dish.setMerchantId(merchantId);
        dishService.add(dish);
        return ResultVO.ok("新增成功");
    }

    /** 修改菜品 */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestBody Dish dish,
                                  @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        dishService.update(merchantId, id, dish);
        return ResultVO.ok("修改成功");
    }

    /** 删除菜品 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        dishService.delete(merchantId, id);
        return ResultVO.ok("删除成功");
    }

    /** 查询菜品详情 */
    @GetMapping("/{id}")
    public ResultVO<Dish> detail(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestHeader(value = "X-Shop-Id", required = false) Long shopId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long merchantId = getMerchantId(loginUser, shopId);
        Dish dish = dishService.findById(id);
        // 校验该菜品是否属于当前商家
        if (!dish.getMerchantId().equals(merchantId)) {
            return ResultVO.error(403, "无权访问该菜品");
        }
        return ResultVO.success(dish);
    }

}
