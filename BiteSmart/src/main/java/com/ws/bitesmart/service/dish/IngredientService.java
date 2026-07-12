package com.ws.bitesmart.service.dish;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Ingredient;
import com.ws.bitesmart.mapper.dish.IngredientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 食材服务类
 *
 * 提供食材的增删改查业务逻辑，包括：
 * 1. 管理员维护食材库（增删改）
 * 2. 商家查询可用食材（按分类、全部）
 * 3. 查询所有分类名称
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientMapper ingredientMapper;

    /**
     * 查询所有启用的食材列表
     * 用于商家端选择食材时展示
     *
     * @return 启用的食材列表
     */
    public List<Ingredient> findAllEnabled() {
        return ingredientMapper.findAllEnabled();
    }

    /**
     * 按分类查询食材列表
     * 商家可以按分类筛选食材
     *
     * @param categoryName 分类名称，如：肉类、蔬菜等
     * @return 该分类下的食材列表
     */
    public List<Ingredient> findByCategory(String categoryName) {
        return ingredientMapper.findByCategory(categoryName);
    }

    /**
     * 分页查询所有启用的食材
     * 用于管理员后台管理食材库
     *
     * @param pageNum  页码，从1开始
     * @param pageSize 每页条数
     * @return 分页后的食材列表
     */
    public PageInfo<Ingredient> findAllEnabled(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Ingredient> list = ingredientMapper.findAllEnabled();
        return new PageInfo<>(list);
    }

    /**
     * 根据ID查询单个食材
     * 用于编辑食材或查看食材详情
     *
     * @param id 食材ID
     * @return 食材实体，不存在则返回null
     */
    public Ingredient findById(Long id) {
        return ingredientMapper.findById(id);
    }

    /**
     * 新增食材
     * 管理员在后台添加新食材到食材库
     *
     * @param ingredient 食材实体
     */
    @Transactional
    public void add(Ingredient ingredient) {
        // 使用雪花算法生成ID
        ingredient.setId(SnowflakeUtil.generate());
        // 默认状态为启用
        if (ingredient.getStatus() == null) {
            ingredient.setStatus(10);
        }
        ingredientMapper.insert(ingredient);
        log.info("新增食材成功：id={}, name={}", ingredient.getId(), ingredient.getName());
    }

    /**
     * 更新食材信息
     * 管理员修改食材的营养成分、名称等信息
     *
     * @param ingredient 食材实体
     */
    @Transactional
    public void update(Ingredient ingredient) {
        ingredientMapper.updateById(ingredient);
        log.info("更新食材成功：id={}", ingredient.getId());
    }

    /**
     * 删除食材（逻辑删除）
     * 不是物理删除，只是将 deleted 字段设为 1
     *
     * @param id 食材ID
     */
    @Transactional
    public void delete(Long id) {
        ingredientMapper.deleteById(id);
        log.info("删除食材成功：id={}", id);
    }

    /**
     * 查询所有分类名称
     * 用于前端展示分类筛选下拉框
     *
     * @return 分类名称列表
     */
    public List<String> findAllCategories() {
        return ingredientMapper.findAllCategories();
    }

}
