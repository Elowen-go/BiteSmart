package com.ws.bitesmart.controller.order;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.order.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口
 *
 * 用户端：查看购物车、添加商品、修改数量、切换选中、删除商品。
 * 下单时只购买 selected=1 的商品，跟淘宝购物车逻辑一样。
 */
@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    /** 查我的购物车 */
    @GetMapping
    public ResultVO<List<ShoppingCart>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(shoppingCartService.findByUserId(loginUser.getUserId()));
    }

    /**
     * 加商品到购物车
     *
     * @param dishId   菜品ID（itemType=10时必传）
     * @param comboId  套餐ID（itemType=20时必传）
     * @param itemType 商品类型：10-菜品 20-套餐
     * @param quantity 数量
     */
    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestParam(required = false) Long dishId,
                               @RequestParam(required = false) Long comboId,
                               @RequestParam Integer itemType,
                               @RequestParam(defaultValue = "1") Integer quantity) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.add(loginUser.getUserId(), itemType, dishId, comboId, quantity);
        return ResultVO.ok("添加成功");
    }

    /** 改数量 */
    @PutMapping("/{id}")
    public ResultVO<Void> updateQuantity(@AuthenticationPrincipal LoginUser loginUser,
                                          @PathVariable Long id,
                                          @RequestParam Integer quantity) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.updateQuantity(id, loginUser.getUserId(), quantity);
        return ResultVO.ok("修改成功");
    }

    /**
     * 切换商品选中状态
     * PUT /api/cart/{id}/select?selected=1
     * selected=1 选中，selected=0 取消选中
     */
    @PutMapping("/{id}/select")
    public ResultVO<Void> select(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id,
                                  @RequestParam Integer selected) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.updateSelected(id, loginUser.getUserId(), selected);
        return ResultVO.ok("操作成功");
    }

    /** 删商品 */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.deleteById(id, loginUser.getUserId());
        return ResultVO.ok("删除成功");
    }
}
