package com.ws.bitesmart.service.dish;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐服务
 *
 * 商家端：创建套餐（含关联菜品）、更新套餐（先删旧关联再插新关联）、上下架。
 * 套餐类型：10-减脂 20-增肌 30-控糖 40-会员专属
 * 按 merchantId 进行数据隔离。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboMapper comboMapper;
    private final ComboDishRelMapper comboDishRelMapper;
    private final DishMapper dishMapper;

    /** 查某商家的全部套餐 */
    public List<Combo> findByMerchantId(Long merchantId) {
        return comboMapper.findByMerchantId(merchantId);
    }

    /** 查套餐详情 */
    public Combo findById(Long id) {
        Combo combo = comboMapper.findById(id);
        if (combo == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        return combo;
    }

    /** 查询上架套餐 */
    public List<Combo> findAvailable() {
        return comboMapper.findAvailable();
    }

    /** 查询上架套餐（分页） */
    public PageInfo<Combo> findAvailable(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Combo> list = comboMapper.findAvailable();
        return new PageInfo<>(list);
    }

    /**
     * 新增套餐
     *
     * @param combo   套餐信息
     * @param dishIds 关联的菜品 ID 列表
     */
    @Transactional
    public void add(Combo combo, List<Long> dishIds) {
        combo.setId(SnowflakeUtil.generate());
        if (combo.getStatus() == null) combo.setStatus(10); // 默认上架
        comboMapper.insert(combo);

        // 批量插入关联
        if (dishIds != null && !dishIds.isEmpty()) {
            List<ComboDishRel> relList = new ArrayList<>();
            for (Long dishId : dishIds) {
                ComboDishRel rel = new ComboDishRel();
                rel.setId(SnowflakeUtil.generate());
                rel.setComboId(combo.getId());
                rel.setDishId(dishId);
                rel.setQuantity(1); // 默认 1 份
                rel.setIsFixed(1);  // 默认固定不可替换
                relList.add(rel);
            }
            comboDishRelMapper.insertBatch(relList);
        }
    }

    /**
     * 修改套餐
     * 先删旧关联，再插新关联
     *
     * @param merchantId 商家 ID
     * @param id         套餐 ID
     * @param combo      套餐信息
     * @param dishIds    新的关联菜品 ID 列表
     */
    @Transactional
    public void update(Long merchantId, Long id, Combo combo, List<Long> dishIds) {
        Combo exist = comboMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        combo.setId(id);
        combo.setMerchantId(merchantId);
        comboMapper.updateById(combo);

        // 先删旧关联
        comboDishRelMapper.deleteByComboId(id);

        // 再插新关联
        if (dishIds != null && !dishIds.isEmpty()) {
            List<ComboDishRel> relList = new ArrayList<>();
            for (Long dishId : dishIds) {
                ComboDishRel rel = new ComboDishRel();
                rel.setId(SnowflakeUtil.generate());
                rel.setComboId(id);
                rel.setDishId(dishId);
                rel.setQuantity(1);
                rel.setIsFixed(1);
                relList.add(rel);
            }
            comboDishRelMapper.insertBatch(relList);
        }
    }

    /** 下架套餐，校验所属权 */
    @Transactional
    public void delete(Long merchantId, Long id) {
        Combo exist = comboMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        Combo update = new Combo();
        update.setId(id);
        update.setStatus(20); // 下架
        comboMapper.updateById(update);
    }

    /** 查套餐包含的菜品关联列表 */
    public List<ComboDishRel> findRelByComboId(Long comboId) {
        return comboDishRelMapper.findByComboId(comboId);
    }

    /**
     * 套餐换菜
     *
     * 用户在购买套餐时，可以把套餐内可替换的菜品换成替换池里的其他菜品。
     * 换完后重新计算套餐的总热量和营养素。
     *
     * @param comboId   套餐ID
     * @param oldDishId 要换掉的菜品ID
     * @param newDishId 替换成的菜品ID（必须在 replaceable_dish_pool 里）
     * @return 换菜后的套餐信息（含调整后的营养数据）
     */
    @Transactional
    public Map<String, Object> replaceDish(Long comboId, Long oldDishId, Long newDishId) {
        // 1. 查套餐
        Combo combo = comboMapper.findById(comboId);
        if (combo == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        if (combo.getMaxReplaceCount() == null || combo.getMaxReplaceCount() <= 0) {
            throw new BusinessException("该套餐不支持换菜");
        }

        // 2. 查替换池
        String poolJson = combo.getReplaceableDishPool();
        if (poolJson == null || poolJson.isEmpty()) {
            throw new BusinessException("该套餐无可替换菜品");
        }
        List<Long> pool = JSON.parseArray(poolJson, Long.class);
        if (!pool.contains(newDishId)) {
            throw new BusinessException("替换的菜品不在可替换池中");
        }

        // 3. 查套餐包含的菜品关联，校验 oldDishId 是否在套餐中且可替换
        List<ComboDishRel> rels = comboDishRelMapper.findByComboId(comboId);
        ComboDishRel targetRel = null;
        for (ComboDishRel rel : rels) {
            if (rel.getDishId().equals(oldDishId)) {
                targetRel = rel;
                break;
            }
        }
        if (targetRel == null) {
            throw new BusinessException("该菜品不在当前套餐中");
        }
        if (targetRel.getIsFixed() == 1) {
            throw new BusinessException("该菜品为固定菜品，不可替换");
        }

        // 4. 新旧菜品的营养数据
        Dish oldDish = dishMapper.findById(oldDishId);
        Dish newDish = dishMapper.findById(newDishId);
        if (oldDish == null || newDish == null) {
            throw new BusinessException("菜品不存在");
        }

        // 5. 重新计算套餐营养数据
        int calDiff = (newDish.getCalories() != null ? newDish.getCalories() : 0)
                - (oldDish.getCalories() != null ? oldDish.getCalories() : 0);
        BigDecimal proteinDiff = safeSub(newDish.getProtein(), oldDish.getProtein());
        BigDecimal fatDiff = safeSub(newDish.getFat(), oldDish.getFat());
        BigDecimal carbsDiff = safeSub(newDish.getCarbs(), oldDish.getCarbs());

        int newCalories = (combo.getTotalCalories() != null ? combo.getTotalCalories() : 0) + calDiff;
        BigDecimal newProtein = safeAdd(combo.getTotalProtein(), proteinDiff);
        BigDecimal newFat = safeAdd(combo.getTotalFat(), fatDiff);
        BigDecimal newCarbs = safeAdd(combo.getTotalCarbs(), carbsDiff);

        // 6. 更新套餐关联（替换菜品ID）
        targetRel.setDishId(newDishId);
        comboDishRelMapper.deleteByComboId(comboId);
        comboDishRelMapper.insertBatch(rels);

        log.info("套餐换菜: comboId={}, {}→{}, diffCal={}", comboId, oldDishId, newDishId, calDiff);

        // 7. 返回调整后的信息
        Map<String, Object> result = new HashMap<>();
        result.put("comboId", comboId);
        result.put("oldDishId", oldDishId);
        result.put("oldDishName", oldDish.getDishName());
        result.put("newDishId", newDishId);
        result.put("newDishName", newDish.getDishName());
        result.put("totalCalories", newCalories);
        result.put("totalProtein", newProtein);
        result.put("totalFat", newFat);
        result.put("totalCarbs", newCarbs);
        return result;
    }

    /** 安全减法 */
    private BigDecimal safeSub(BigDecimal a, BigDecimal b) {
        return (a != null ? a : BigDecimal.ZERO).subtract(b != null ? b : BigDecimal.ZERO);
    }

    /** 安全加法 */
    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return (a != null ? a : BigDecimal.ZERO).add(b != null ? b : BigDecimal.ZERO);
    }

}
