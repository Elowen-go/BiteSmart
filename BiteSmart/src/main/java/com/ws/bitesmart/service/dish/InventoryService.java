package com.ws.bitesmart.service.dish;

import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.dish.InventoryLog;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.dish.InventoryLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存管理服务
 *
 * 提供库存预警查询、库存变动记录查询等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final DishMapper dishMapper;
    private final InventoryLogMapper inventoryLogMapper;

    /**
     * 查询某商家库存低于预警阈值的菜品列表
     *
     * @param merchantId 商家ID
     * @return 预警菜品列表
     */
    public List<Dish> getStockWarnings(Long merchantId) {
        List<Dish> dishes = dishMapper.findByMerchantId(merchantId);
        return dishes.stream()
                .filter(d -> d.getStock() != null
                        && d.getMinStockWarning() != null
                        && d.getStock() <= d.getMinStockWarning())
                .collect(Collectors.toList());
    }

    /**
     * 查询某商家的库存变动记录
     *
     * @param merchantId 商家ID
     * @return 库存变动记录列表
     */
    public List<InventoryLog> getInventoryLogs(Long merchantId) {
        return inventoryLogMapper.findByMerchantId(merchantId);
    }

}
