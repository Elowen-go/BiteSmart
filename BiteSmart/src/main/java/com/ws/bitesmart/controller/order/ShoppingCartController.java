package com.ws.bitesmart.controller.order;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.dto.order.ComboCustomizationSnapshot;
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

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @GetMapping
    public ResultVO<List<ShoppingCart>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(shoppingCartService.findByUserId(loginUser.getUserId()));
    }

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

    @PutMapping("/{id}")
    public ResultVO<Void> updateQuantity(@AuthenticationPrincipal LoginUser loginUser,
                                         @PathVariable Long id,
                                         @RequestParam Integer quantity) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.updateQuantity(id, loginUser.getUserId(), quantity);
        return ResultVO.ok("修改成功");
    }

    @PutMapping("/{id}/select")
    public ResultVO<Void> select(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long id,
                                 @RequestParam Integer selected) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.updateSelected(id, loginUser.getUserId(), selected);
        return ResultVO.ok("操作成功");
    }

    @PutMapping("/{id}/replace")
    public ResultVO<ComboCustomizationSnapshot> replaceComboDish(@AuthenticationPrincipal LoginUser loginUser,
                                                                 @PathVariable Long id,
                                                                 @RequestParam Long oldDishId,
                                                                 @RequestParam Long newDishId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(shoppingCartService.replaceComboDish(id, loginUser.getUserId(), oldDishId, newDishId));
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        shoppingCartService.deleteById(id, loginUser.getUserId());
        return ResultVO.ok("删除成功");
    }
}
