package com.ws.bitesmart.service.dish;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.order.ComboCustomizationSnapshot;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboMapper comboMapper;
    private final ComboDishRelMapper comboDishRelMapper;
    private final DishMapper dishMapper;
    private final NutritionCalculateService nutritionCalculateService;

    public List<Combo> findByMerchantId(Long merchantId) {
        return comboMapper.findByMerchantId(merchantId);
    }

    public PageInfo<Combo> findByMerchantId(Long merchantId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(comboMapper.findByMerchantId(merchantId));
    }

    public Combo findById(Long id) {
        Combo combo = comboMapper.findById(id);
        if (combo == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        return combo;
    }

    public List<Combo> findAvailable() {
        return comboMapper.findAvailable();
    }

    public PageInfo<Combo> findAvailable(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(comboMapper.findAvailable());
    }

    @Transactional
    public void add(Combo combo, List<ComboDishRel> dishItems) {
        combo.setId(SnowflakeUtil.generate());
        if (combo.getStatus() == null) combo.setStatus(10);
        if (combo.getSalesCount() == null) combo.setSalesCount(0);
        comboMapper.insert(combo);

        if (dishItems != null && !dishItems.isEmpty()) {
            normalizeDishItems(combo.getId(), dishItems);
            comboDishRelMapper.insertBatch(dishItems);
            nutritionCalculateService.updateComboNutrition(combo.getId());
        }
    }

    @Transactional
    public void update(Long merchantId, Long id, Combo combo, List<ComboDishRel> dishItems) {
        Combo exist = comboMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        combo.setId(id);
        combo.setMerchantId(merchantId);
        comboMapper.updateById(combo);

        if (dishItems != null) {
            comboDishRelMapper.deleteByComboId(id);
            if (!dishItems.isEmpty()) {
                normalizeDishItems(id, dishItems);
                comboDishRelMapper.insertBatch(dishItems);
            }
            nutritionCalculateService.updateComboNutrition(id);
        }
    }

    @Transactional
    public void delete(Long merchantId, Long id) {
        Combo exist = comboMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        comboDishRelMapper.deleteByComboId(id);
        Combo update = new Combo();
        update.setId(id);
        update.setDeleted(1);
        comboMapper.updateById(update);
    }

    public List<ComboDishRel> findRelByComboId(Long comboId) {
        return comboDishRelMapper.findByComboId(comboId);
    }

    public Map<String, Object> replaceDish(Long comboId, Long oldDishId, Long newDishId) {
        ComboCustomizationSnapshot.ReplacementItem replacement = new ComboCustomizationSnapshot.ReplacementItem();
        replacement.setOldDishId(oldDishId);
        replacement.setNewDishId(newDishId);
        ComboCustomizationSnapshot snapshot = buildCustomizedSnapshot(comboId, List.of(replacement));

        Map<String, Object> result = new HashMap<>();
        result.put("comboId", comboId);
        result.put("oldDishId", oldDishId);
        result.put("newDishId", newDishId);
        result.put("totalCalories", snapshot.getTotalCalories());
        result.put("totalProtein", snapshot.getTotalProtein());
        result.put("totalFat", snapshot.getTotalFat());
        result.put("totalCarbs", snapshot.getTotalCarbs());
        result.put("items", snapshot.getItems());
        result.put("replacements", snapshot.getReplacements());
        result.put("customizationJson", JSON.toJSONString(snapshot));
        return result;
    }

    public ComboCustomizationSnapshot buildCustomizedSnapshot(Long comboId,
                                                              List<ComboCustomizationSnapshot.ReplacementItem> replacements) {
        Combo combo = comboMapper.findById(comboId);
        if (combo == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        List<ComboDishRel> rels = cloneRels(comboDishRelMapper.findByComboId(comboId));
        List<Long> pool = parseReplaceablePool(combo.getReplaceableDishPool());
        List<ComboCustomizationSnapshot.ReplacementItem> normalized = new ArrayList<>();

        if (replacements != null && !replacements.isEmpty()) {
            if (combo.getMaxReplaceCount() == null || combo.getMaxReplaceCount() <= 0) {
                throw new BusinessException("该套餐不支持换菜");
            }
            if (replacements.size() > combo.getMaxReplaceCount()) {
                throw new BusinessException("超出套餐可替换数量限制");
            }
            for (ComboCustomizationSnapshot.ReplacementItem replacement : replacements) {
                applyReplacement(combo, rels, pool, replacement, normalized);
            }
        }

        NutritionCalculateService.NutritionSummary nutrition = nutritionCalculateService.calculateComboNutrition(rels);
        ComboCustomizationSnapshot snapshot = new ComboCustomizationSnapshot();
        snapshot.setComboId(comboId);
        snapshot.setReplacements(normalized);
        snapshot.setTotalCalories(nutrition.getCalories());
        snapshot.setTotalProtein(nutrition.getProtein());
        snapshot.setTotalFat(nutrition.getFat());
        snapshot.setTotalCarbs(nutrition.getCarbs());
        for (ComboDishRel rel : rels) {
            Dish dish = dishMapper.findById(rel.getDishId());
            if (dish == null) continue;
            ComboCustomizationSnapshot.SelectedDishItem item = new ComboCustomizationSnapshot.SelectedDishItem();
            item.setDishId(dish.getId());
            item.setDishName(dish.getDishName());
            item.setQuantity(rel.getQuantity() == null ? 1 : rel.getQuantity());
            snapshot.getItems().add(item);
        }
        return snapshot;
    }

    private void applyReplacement(Combo combo,
                                  List<ComboDishRel> rels,
                                  List<Long> pool,
                                  ComboCustomizationSnapshot.ReplacementItem replacement,
                                  List<ComboCustomizationSnapshot.ReplacementItem> normalized) {
        ComboDishRel targetRel = findRel(rels, replacement.getOldDishId());
        if (targetRel == null) {
            throw new BusinessException("该菜品不在当前套餐中");
        }
        if (targetRel.getIsFixed() != null && targetRel.getIsFixed() == 1) {
            throw new BusinessException("该菜品为固定菜品，不可替换");
        }
        if (!pool.isEmpty() && !pool.contains(replacement.getNewDishId())) {
            throw new BusinessException("替换的菜品不在可替换池中");
        }
        Dish oldDish = dishMapper.findById(replacement.getOldDishId());
        Dish newDish = dishMapper.findById(replacement.getNewDishId());
        if (oldDish == null || newDish == null) {
            throw new BusinessException("菜品不存在");
        }
        if (!newDish.getMerchantId().equals(combo.getMerchantId())) {
            throw new BusinessException("替换菜品必须属于同一商家");
        }
        targetRel.setDishId(newDish.getId());

        ComboCustomizationSnapshot.ReplacementItem item = new ComboCustomizationSnapshot.ReplacementItem();
        item.setOldDishId(oldDish.getId());
        item.setOldDishName(oldDish.getDishName());
        item.setNewDishId(newDish.getId());
        item.setNewDishName(newDish.getDishName());
        normalized.add(item);
    }

    private List<ComboDishRel> cloneRels(List<ComboDishRel> rels) {
        List<ComboDishRel> cloned = new ArrayList<>();
        if (rels == null) return cloned;
        for (ComboDishRel rel : rels) {
            ComboDishRel copy = new ComboDishRel();
            copy.setId(rel.getId());
            copy.setComboId(rel.getComboId());
            copy.setDishId(rel.getDishId());
            copy.setQuantity(rel.getQuantity());
            copy.setIsFixed(rel.getIsFixed());
            cloned.add(copy);
        }
        return cloned;
    }

    private List<Long> parseReplaceablePool(String poolJson) {
        if (poolJson == null || poolJson.isEmpty()) {
            return List.of();
        }
        return JSON.parseArray(poolJson, Long.class);
    }

    private ComboDishRel findRel(List<ComboDishRel> rels, Long dishId) {
        for (ComboDishRel rel : rels) {
            if (rel.getDishId().equals(dishId)) {
                return rel;
            }
        }
        return null;
    }

    private void normalizeDishItems(Long comboId, List<ComboDishRel> dishItems) {
        for (ComboDishRel item : dishItems) {
            item.setId(SnowflakeUtil.generate());
            item.setComboId(comboId);
            if (item.getQuantity() == null) item.setQuantity(1);
            if (item.getIsFixed() == null) item.setIsFixed(1);
        }
    }
}
