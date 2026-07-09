package com.ws.bitesmart.service.dish;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.DishCategory;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.DishCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜品分类服务
 *
 * 菜品分类是商家公用的基础数据，不需要按商家隔离。
 * 分类列表按 sortOrder 排序，支持多级分类结构。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishCategoryService {

    private final DishCategoryMapper dishCategoryMapper;

    /** 查询全部分类，按 sort_order 排序 */
    public List<DishCategory> findAll() {
        return dishCategoryMapper.findAll();
    }

    /** 根据 ID 查询分类 */
    public DishCategory findById(Long id) {
        DishCategory category = dishCategoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    /** 新增分类 */
    @Transactional
    public void add(DishCategory category) {
        category.setId(SnowflakeUtil.generate());
        dishCategoryMapper.insert(category);
    }

    /** 修改分类 */
    @Transactional
    public void update(Long id, DishCategory category) {
        DishCategory exist = dishCategoryMapper.findById(id);
        if (exist == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "分类不存在");
        }
        category.setId(id);
        dishCategoryMapper.updateById(category);
    }

    /** 删除分类（软删除） */
    @Transactional
    public void delete(Long id) {
        DishCategory exist = dishCategoryMapper.findById(id);
        if (exist == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "分类不存在");
        }
        dishCategoryMapper.deleteById(id);
    }

}
