package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.Ingredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 食材 Mapper 接口
 *
 * 提供食材的增删改查操作，包括按分类查询、分页查询等功能。
 * 主要用于管理员维护食材库，以及商家查询可用食材。
 */
@Mapper
public interface IngredientMapper {

    /**
     * 查询所有启用的食材列表
     * 用于商家端选择食材时展示
     *
     * @return 启用的食材列表（不包含已删除和禁用的）
     */
    List<Ingredient> findAllEnabled();

    /**
     * 按分类查询食材列表
     * 商家可以按分类筛选食材，如只看肉类、只看蔬菜等
     *
     * @param categoryName 分类名称，如：肉类、蔬菜、主食等
     * @return 该分类下的食材列表
     */
    List<Ingredient> findByCategory(@Param("categoryName") String categoryName);

    /**
     * 根据ID查询单个食材
     * 用于编辑食材或查看食材详情
     *
     * @param id 食材ID
     * @return 食材实体，不存在则返回null
     */
    Ingredient findById(@Param("id") Long id);

    /**
     * 新增食材
     * 管理员在后台添加新食材到食材库
     *
     * @param ingredient 食材实体
     * @return 影响的行数，成功为1
     */
    int insert(Ingredient ingredient);

    /**
     * 更新食材信息
     * 管理员修改食材的营养成分、名称等信息
     * 使用动态SQL，只更新非空字段
     *
     * @param ingredient 食材实体
     * @return 影响的行数，成功为1
     */
    int updateById(Ingredient ingredient);

    /**
     * 逻辑删除食材
     * 不是物理删除，只是将 deleted 字段设为 1
     *
     * @param id 食材ID
     * @return 影响的行数，成功为1
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询所有分类名称
     * 用于前端展示分类筛选下拉框
     *
     * @return 分类名称列表，如：[肉类, 蔬菜, 主食, 海鲜]
     */
    List<String> findAllCategories();

}
