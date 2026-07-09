package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.DishCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品分类 Mapper
 *
 * 菜品分类是商家公用的基础数据，支持多级分类结构。
 */
@Mapper
public interface DishCategoryMapper {

    /** 查询全部分类，按 sortOrder 排序 */
    List<DishCategory> findAll();

    /** 根据 ID 查询分类 */
    DishCategory findById(@Param("id") Long id);

    /** 新增分类 */
    int insert(DishCategory category);

    /** 修改分类（动态 SQL，只改非空字段） */
    int updateById(DishCategory category);

    /** 软删除分类 */
    int deleteById(@Param("id") Long id);

}
