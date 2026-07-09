package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品 Mapper
 *
 * 支持按商家维度的查询、上架状态筛选、库存更新等操作。
 */
@Mapper
public interface DishMapper {

    /** 查某商家的全部菜品，按创建时间倒序 */
    List<Dish> findByMerchantId(@Param("merchantId") Long merchantId);

    /** 根据 ID 查询菜品 */
    Dish findById(@Param("id") Long id);

    /** 查询上架且在售的菜品（status=10 且 stock>0） */
    List<Dish> findAvailable();

    /** 新增菜品 */
    int insert(Dish dish);

    /** 修改菜品（动态 SQL，只改非空字段） */
    int updateById(Dish dish);

    /** 下单锁定库存（乐观锁：stock >= quantity 时才扣减，返回0表示库存不足） */
    int lockStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 取消/退款释放锁定库存 */
    int unlockStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 支付成功扣减实际库存 */
    int deductLockedStock(@Param("id") Long id, @Param("quantity") Integer quantity);

}
