package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.dish.InventoryLog;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.dish.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 库存管理接口
 *
 * 商家查看库存预警列表、库存变动记录。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/inventory")
@RequiredArgsConstructor
public class MerchantInventoryController {

    private final InventoryService inventoryService;

    /** 库存预警列表（库存低于预警阈值的菜品） */
    @GetMapping("/warnings")
    public ResultVO<List<Dish>> warnings(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(inventoryService.getStockWarnings(loginUser.getUserId()));
    }

    /** 库存变动记录 */
    @GetMapping("/logs")
    public ResultVO<List<InventoryLog>> logs(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(inventoryService.getInventoryLogs(loginUser.getUserId()));
    }

}
