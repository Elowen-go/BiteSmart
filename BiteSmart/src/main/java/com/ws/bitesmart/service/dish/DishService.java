package com.ws.bitesmart.service.dish;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.DishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        dish.setId(SnowflakeUtil.generate());
        dish.setSalesCount(0);
        dish.setSalesReal(0);
        if (dish.getLockStock() == null) dish.setLockStock(0);
        if (dish.getStatus() == null) dish.setStatus(10); // 默认上架
        dishMapper.insert(dish);
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
    }

    /** 下架菜品（软删除/下架），校验所属权 */
    @Transactional
    public void delete(Long merchantId, Long id) {
        Dish exist = dishMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品不存在");
        }
        Dish update = new Dish();
        update.setId(id);
        update.setStatus(20); // 下架
        dishMapper.updateById(update);
    }

}
