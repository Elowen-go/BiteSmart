package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.DishIngredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品-食材关联 Mapper 接口
 *
 * 提供菜品与食材关联关系的增删改查操作。
 */
@Mapper
public interface DishIngredientMapper {

    /**
     * 批量插入菜品-食材关联
     * 添加菜品时调用，保存该菜品使用的所有食材
     *
     * @param list 关联列表
     * @return 影响的行数
     */
    int batchInsert(@Param("list") List<DishIngredient> list);

    /**
     * 根据菜品ID查询关联的食材
     * 查询菜品详情时调用，获取该菜品使用的所有食材及用量
     *
     * @param dishId 菜品ID
     * @return 关联列表
     */
    List<DishIngredient> findByDishId(@Param("dishId") Long dishId);

    /**
     * 根据菜品ID删除关联
     * 编辑菜品时先删除旧关联，再插入新关联
     *
     * @param dishId 菜品ID
     * @return 影响的行数
     */
    int deleteByDishId(@Param("dishId") Long dishId);

}
