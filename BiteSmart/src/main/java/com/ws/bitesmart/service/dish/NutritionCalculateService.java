package com.ws.bitesmart.service.dish;

import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.dish.DishIngredient;
import com.ws.bitesmart.entity.dish.Ingredient;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishIngredientMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.dish.IngredientMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NutritionCalculateService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DishIngredientMapper dishIngredientMapper;
    private final IngredientMapper ingredientMapper;
    private final DishMapper dishMapper;
    private final ComboDishRelMapper comboDishRelMapper;
    private final ComboMapper comboMapper;

    public NutritionSummary calculateDishNutrition(Long dishId) {
        NutritionSummary summary = new NutritionSummary();
        List<DishIngredient> ingredients = dishIngredientMapper.findByDishId(dishId);
        if (ingredients == null || ingredients.isEmpty()) {
            return summary;
        }

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;

        for (DishIngredient item : ingredients) {
            Ingredient ingredient = ingredientMapper.findById(item.getIngredientId());
            if (ingredient == null || item.getWeight() == null) {
                continue;
            }
            BigDecimal factor = item.getWeight().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
            calories = calories.add(safe(ingredient.getCalories()).multiply(factor));
            protein = protein.add(safe(ingredient.getProtein()).multiply(factor));
            fat = fat.add(safe(ingredient.getFat()).multiply(factor));
            carbs = carbs.add(safe(ingredient.getCarbs()).multiply(factor));
        }

        summary.setCalories(calories.setScale(0, RoundingMode.HALF_UP).intValue());
        summary.setProtein(scale(protein));
        summary.setFat(scale(fat));
        summary.setCarbs(scale(carbs));
        return summary;
    }

    public NutritionSummary updateDishNutrition(Long dishId) {
        NutritionSummary summary = calculateDishNutrition(dishId);
        Dish update = new Dish();
        update.setId(dishId);
        update.setCalories(summary.getCalories());
        update.setProtein(summary.getProtein());
        update.setFat(summary.getFat());
        update.setCarbs(summary.getCarbs());
        dishMapper.updateById(update);
        return summary;
    }

    public NutritionSummary calculateComboNutrition(Long comboId) {
        List<ComboDishRel> rels = comboDishRelMapper.findByComboId(comboId);
        return calculateComboNutrition(rels);
    }

    public NutritionSummary calculateComboNutrition(List<ComboDishRel> rels) {
        NutritionSummary summary = new NutritionSummary();
        if (rels == null || rels.isEmpty()) {
            return summary;
        }
        for (ComboDishRel rel : rels) {
            Dish dish = dishMapper.findById(rel.getDishId());
            if (dish == null) {
                continue;
            }
            summary.addDish(dish, rel.getQuantity());
        }
        summary.normalize();
        return summary;
    }

    public NutritionSummary updateComboNutrition(Long comboId) {
        NutritionSummary summary = calculateComboNutrition(comboId);
        Combo update = new Combo();
        update.setId(comboId);
        update.setTotalCalories(summary.getCalories());
        update.setTotalProtein(summary.getProtein());
        update.setTotalFat(summary.getFat());
        update.setTotalCarbs(summary.getCarbs());
        comboMapper.updateById(update);
        return summary;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    @Data
    @AllArgsConstructor
    public static class NutritionSummary {
        private Integer calories;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbs;

        public NutritionSummary() {
            this.calories = 0;
            this.protein = BigDecimal.ZERO;
            this.fat = BigDecimal.ZERO;
            this.carbs = BigDecimal.ZERO;
        }

        public void addDish(Dish dish, Integer quantity) {
            int qty = quantity == null ? 1 : quantity;
            this.calories += (dish.getCalories() == null ? 0 : dish.getCalories()) * qty;
            BigDecimal factor = BigDecimal.valueOf(qty);
            this.protein = this.protein.add((dish.getProtein() == null ? BigDecimal.ZERO : dish.getProtein()).multiply(factor));
            this.fat = this.fat.add((dish.getFat() == null ? BigDecimal.ZERO : dish.getFat()).multiply(factor));
            this.carbs = this.carbs.add((dish.getCarbs() == null ? BigDecimal.ZERO : dish.getCarbs()).multiply(factor));
        }

        public void normalize() {
            this.protein = this.protein.setScale(2, RoundingMode.HALF_UP);
            this.fat = this.fat.setScale(2, RoundingMode.HALF_UP);
            this.carbs = this.carbs.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
