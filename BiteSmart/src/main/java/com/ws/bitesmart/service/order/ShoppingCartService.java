package com.ws.bitesmart.service.order;

import com.alibaba.fastjson2.JSON;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.order.ComboCustomizationSnapshot;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.order.ShoppingCartMapper;
import com.ws.bitesmart.service.dish.ComboService;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务
 *
 * 用户端：添加商品到购物车、修改数量、删除、查列表。
 * 添加时如已存在相同商品则累加数量，否则新增。
 * itemType：10-菜品 20-套餐
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartMapper shoppingCartMapper;
    private final ComboService comboService;
    private final DishMapper dishMapper;
    private final ComboMapper comboMapper;

    /** 查用户购物车列表 */
    public List<ShoppingCart> findByUserId(Long userId) {
        List<ShoppingCart> items = shoppingCartMapper.findByUserId(userId);
        for (ShoppingCart item : items) {
            if (item.getItemType() != null && item.getItemType() == 10 && item.getDishId() != null) {
                Dish dish = dishMapper.findById(item.getDishId());
                if (dish != null) {
                    item.setDishName(dish.getDishName());
                    item.setDishImage(dish.getDishImage());
                    item.setPrice(dish.getPrice());
                }
            } else if (item.getItemType() != null && item.getItemType() == 20 && item.getComboId() != null) {
                Combo combo = comboMapper.findById(item.getComboId());
                if (combo != null) {
                    item.setComboName(combo.getComboName());
                    item.setComboImage(combo.getComboImage());
                    item.setPrice(combo.getPrice());
                }
            }
        }
        return items;
    }

    /**
     * 添加商品到购物车
     * 如果同一用户的同一商品已存在则累加数量，否则新增
     */
    @Transactional
    public void add(Long userId, Integer itemType, Long dishId, Long comboId, Integer quantity) {
        // 查是否已存在
        ShoppingCart exist = shoppingCartMapper.findByUserIdAndItem(userId, itemType, dishId, comboId);
        if (exist != null) {
            // 已存在则累加数量
            shoppingCartMapper.updateQuantity(exist.getId(), exist.getQuantity() + quantity);
            log.info("购物车商品已存在，累加数量: userId={}, itemType={}, dishId={}, comboId={}, newQty={}",
                    userId, itemType, dishId, comboId, exist.getQuantity() + quantity);
            return;
        }

        // 新增
        ShoppingCart cart = new ShoppingCart();
        cart.setId(SnowflakeUtil.generate());
        cart.setUserId(userId);
        cart.setItemType(itemType);
        cart.setDishId(dishId);
        cart.setComboId(comboId);
        cart.setQuantity(quantity);
        cart.setSelected(1); // 默认选中
        shoppingCartMapper.insert(cart);
        log.info("购物车新增商品: userId={}, itemType={}, dishId={}, comboId={}, qty={}",
                userId, itemType, dishId, comboId, quantity);
    }

    /** 修改数量（校验所属权） */
    @Transactional
    public void updateQuantity(Long id, Long userId, Integer quantity) {
        ShoppingCart cart = shoppingCartMapper.findById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "购物车商品不存在");
        }
        shoppingCartMapper.updateQuantity(id, quantity);
    }

    /** 删除购物车商品（校验所属权） */
    @Transactional
    public void deleteById(Long id, Long userId) {
        ShoppingCart cart = shoppingCartMapper.findById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "购物车商品不存在");
        }
        shoppingCartMapper.deleteById(id);
    }

    /** 查用户选中的购物车商品 */
    public List<ShoppingCart> findSelectedByUserId(Long userId) {
        return shoppingCartMapper.findSelectedByUserId(userId);
    }

    /** 切换选中状态（校验所属权） */
    @Transactional
    public void updateSelected(Long id, Long userId, Integer selected) {
        ShoppingCart cart = shoppingCartMapper.findById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "购物车商品不存在");
        }
        shoppingCartMapper.updateSelected(id, selected);
    }

    @Transactional
    public ComboCustomizationSnapshot replaceComboDish(Long id, Long userId, Long oldDishId, Long newDishId) {
        ShoppingCart cart = shoppingCartMapper.findById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "璐墿杞﹀晢鍝佷笉瀛樺湪");
        }
        if (cart.getItemType() == null || cart.getItemType() != 20 || cart.getComboId() == null) {
            throw new BusinessException("只有套餐支持换菜");
        }

        List<ComboCustomizationSnapshot.ReplacementItem> replacements = loadReplacements(cart.getCustomizationJson());
        boolean updated = false;
        for (ComboCustomizationSnapshot.ReplacementItem item : replacements) {
            if (item.getOldDishId().equals(oldDishId)) {
                item.setNewDishId(newDishId);
                updated = true;
                break;
            }
        }
        if (!updated) {
            ComboCustomizationSnapshot.ReplacementItem item = new ComboCustomizationSnapshot.ReplacementItem();
            item.setOldDishId(oldDishId);
            item.setNewDishId(newDishId);
            replacements.add(item);
        }

        ComboCustomizationSnapshot snapshot = comboService.buildCustomizedSnapshot(cart.getComboId(), replacements);
        shoppingCartMapper.updateCustomization(id, JSON.toJSONString(snapshot));
        return snapshot;
    }

    private List<ComboCustomizationSnapshot.ReplacementItem> loadReplacements(String customizationJson) {
        if (customizationJson == null || customizationJson.isEmpty()) {
            return new ArrayList<>();
        }
        ComboCustomizationSnapshot snapshot = JSON.parseObject(customizationJson, ComboCustomizationSnapshot.class);
        if (snapshot == null || snapshot.getReplacements() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(snapshot.getReplacements());
    }
}
