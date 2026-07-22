package com.ws.bitesmart.dto.request;

import lombok.Data;

/**
 * 计划打卡请求
 */
@Data
public class PlanCheckRequest {

    /** 计划餐次ID */
    private Long mealId;

    /** true-打卡 false-取消打卡 */
    private Boolean checked;

}
