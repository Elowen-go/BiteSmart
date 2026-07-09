package com.ws.bitesmart.mapper.order;

import com.ws.bitesmart.entity.order.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车 Mapper
 *
 * 支持购物车商品的增删改查，以及下单后批量清除选中商品。
 */
@Mapper
public interface ShoppingCartMapper {

    /** 查用户购物车全部商品 */
    List<ShoppingCart> findByUserId(@Param("userId") Long userId);

    /** 查购物车中某个商品是否已存在（按 userId + itemType + dishId/comboId 唯一判断） */
    ShoppingCart findByUserIdAndItem(@Param("userId") Long userId,
                                     @Param("itemType") Integer itemType,
                                     @Param("dishId") Long dishId,
                                     @Param("comboId") Long comboId);

    /** 按ID查购物车商品 */
    ShoppingCart findById(@Param("id") Long id);

    /** 查购物车中已选中的商品 */
    List<ShoppingCart> findSelectedByUserId(@Param("userId") Long userId);

    /** 新增购物车商品 */
    int insert(ShoppingCart cart);

    /** 修改数量 */
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 修改选中状态 */
    int updateSelected(@Param("id") Long id, @Param("selected") Integer selected);

    /** 删除购物车商品（逻辑删除） */
    int deleteById(@Param("id") Long id);

    /** 下单后清除已选中商品（逻辑删除） */
    int deleteByUserId(@Param("userId") Long userId);
}
