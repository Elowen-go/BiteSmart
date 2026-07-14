package com.ws.bitesmart.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AiRecipeCartRequest {
    private List<AiRecipeCartItem> items;

    @Data
    public static class AiRecipeCartItem {
        private Long dishId;
        private Integer quantity = 1;
    }
}
