package com.ws.bitesmart.dto.request;

import lombok.Data;

@Data
public class AiRecommendRequest {
    private String mealType = "all";
    private String dietaryRestrictions;
}
