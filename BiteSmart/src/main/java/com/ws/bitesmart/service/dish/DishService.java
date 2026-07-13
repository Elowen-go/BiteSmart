package com.ws.bitesmart.service.dish;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.dish.DishIngredient;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.dish.DishIngredientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜品服务
 *
 * 商家端：创建、修改、上下架自己的菜品。
 * 用户端（通过 DishController）：浏览上架菜品。
 * 按 merchantId 进行数据隔离。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;
    private final DishIngredientMapper dishIngredientMapper;
    private final NutritionCalculateService nutritionCalculateService;

    /** 查某商家的全部菜品 */
    public List<Dish> findByMerchantId(Long merchantId) {
        return dishMapper.findByMerchantId(merchantId);
    }

    /** 查某商家的全部菜品（分页） */
    public PageInfo<Dish> findByMerchantId(Long merchantId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Dish> list = dishMapper.findByMerchantId(merchantId);
        return new PageInfo<>(list);
    }

    /** 查菜品详情 */
    public Dish findById(Long id) {
        Dish dish = dishMapper.findById(id);
        if (dish == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品不存在");
        }
        // 加载关联的食材
        List<DishIngredient> ingredients = dishIngredientMapper.findByDishId(id);
        dish.setIngredients(ingredients);
        return dish;
    }

    /** 查询上架且在售的菜品 */
    public List<Dish> findAvailable() {
        return dishMapper.findAvailable();
    }

    /** 查询上架且在售的菜品（分页） */
    public PageInfo<Dish> findAvailable(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Dish> list = dishMapper.findAvailable();
        return new PageInfo<>(list);
    }

    /** 新增菜品 */
    @Transactional
    public void add(Dish dish) {
        Long dishId = SnowflakeUtil.generate();
        dish.setId(dishId);
        dish.setSalesCount(0);
        dish.setSalesReal(0);
        if (dish.getLockStock() == null) dish.setLockStock(0);
        if (dish.getStatus() == null) dish.setStatus(10); // 默认上架
        dishMapper.insert(dish);
        
        // 保存菜品-食材关联
        saveDishIngredients(dishId, dish.getIngredients());
        if (dish.getIngredients() != null && !dish.getIngredients().isEmpty()) {
            nutritionCalculateService.updateDishNutrition(dishId);
        }
    }

    /** 修改菜品，校验所属权 */
    @Transactional
    public void update(Long merchantId, Long id, Dish dish) {
        Dish exist = dishMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品不存在");
        }
        dish.setId(id);
        dish.setMerchantId(merchantId);
        dishMapper.updateById(dish);
        
        // 更新菜品-食材关联：先删除旧关联，再插入新关联
        if (dish.getIngredients() != null) {
            dishIngredientMapper.deleteByDishId(id);
            saveDishIngredients(id, dish.getIngredients());
            nutritionCalculateService.updateDishNutrition(id);
        }
    }
    
    /**
     * 保存菜品-食材关联
     * @param dishId 菜品ID
     * @param ingredients 食材列表
     */
    private void saveDishIngredients(Long dishId, List<DishIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        List<DishIngredient> list = new ArrayList<>();
        for (DishIngredient item : ingredients) {
            DishIngredient di = new DishIngredient();
            di.setId(SnowflakeUtil.generate());
            di.setDishId(dishId);
            di.setIngredientId(item.getIngredientId());
            di.setWeight(item.getWeight());
            list.add(di);
        }
        dishIngredientMapper.batchInsert(list);
        log.info("保存菜品-食材关联：dishId={}, count={}", dishId, list.size());
    }

    /** 删除菜品（软删除），校验所属权 */
    @Transactional
    public void delete(Long merchantId, Long id) {
        Dish exist = dishMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品不存在");
        }
        // 软删除菜品-食材关联
        dishIngredientMapper.deleteByDishId(id);
        // 软删除菜品
        Dish update = new Dish();
        update.setId(id);
        update.setDeleted(1);
        dishMapper.updateById(update);
    }

}
