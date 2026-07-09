package com.ws.bitesmart.service.order;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.order.ShoppingCartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 查用户购物车列表 */
    public List<ShoppingCart> findByUserId(Long userId) {
        return shoppingCartMapper.findByUserId(userId);
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
}
