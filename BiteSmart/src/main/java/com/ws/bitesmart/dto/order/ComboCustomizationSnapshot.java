package com.ws.bitesmart.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ComboCustomizationSnapshot {

    private Long comboId;
    private List<ReplacementItem> replacements = new ArrayList<>();
    private List<SelectedDishItem> items = new ArrayList<>();
    private Integer totalCalories = 0;
    private BigDecimal totalProtein = BigDecimal.ZERO;
    private BigDecimal totalFat = BigDecimal.ZERO;
    private BigDecimal totalCarbs = BigDecimal.ZERO;

    @Data
    public static class ReplacementItem {
        private Long oldDishId;
        private String oldDishName;
        private Long newDishId;
        private String newDishName;
    }

    @Data
    public static class SelectedDishItem {
        private Long dishId;
        private String dishName;
        private Integer quantity = 1;
    }
}
