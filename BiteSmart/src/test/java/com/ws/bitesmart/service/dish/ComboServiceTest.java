package com.ws.bitesmart.service.dish;

import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComboServiceTest {

    @Mock private ComboMapper comboMapper;
    @Mock private ComboDishRelMapper comboDishRelMapper;
    @Mock private DishMapper dishMapper;
    @Mock private NutritionCalculateService nutritionCalculateService;

    @Test
    void replacementRejectsDishAlreadyUsedByAnotherComboSlot() {
        Combo combo = new Combo();
        combo.setId(1L);
        combo.setMerchantId(10L);
        combo.setMaxReplaceCount(1);
        combo.setReplaceableDishPool("[102]");
        ComboDishRel source = relation(101L, 0);
        ComboDishRel occupied = relation(102L, 0);
        Dish oldDish = dish(101L, 10L, "原菜品");
        Dish newDish = dish(102L, 10L, "已存在菜品");
        when(comboMapper.findById(1L)).thenReturn(combo);
        when(comboDishRelMapper.findByComboId(1L)).thenReturn(List.of(source, occupied));
        when(dishMapper.findById(101L)).thenReturn(oldDish);
        when(dishMapper.findById(102L)).thenReturn(newDish);

        assertThatThrownBy(() -> service().buildCustomizedSnapshot(1L, List.of(replacement(101L, 102L))))
                .hasMessageContaining("不能重复选择");
    }

    private ComboService service() {
        return new ComboService(comboMapper, comboDishRelMapper, dishMapper, nutritionCalculateService);
    }

    private ComboDishRel relation(Long dishId, int fixed) {
        ComboDishRel relation = new ComboDishRel();
        relation.setComboId(1L);
        relation.setDishId(dishId);
        relation.setQuantity(1);
        relation.setIsFixed(fixed);
        return relation;
    }

    private Dish dish(Long id, Long merchantId, String name) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setMerchantId(merchantId);
        dish.setDishName(name);
        return dish;
    }

    private com.ws.bitesmart.dto.order.ComboCustomizationSnapshot.ReplacementItem replacement(Long oldId, Long newId) {
        var replacement = new com.ws.bitesmart.dto.order.ComboCustomizationSnapshot.ReplacementItem();
        replacement.setOldDishId(oldId);
        replacement.setNewDishId(newId);
        return replacement;
    }
}
