package com.ws.bitesmart.dto.request;

import lombok.Data;

/**
 * 生成专属计划请求
 */
@Data
public class PlanGenerateRequest {

    /** 计划天数：7 或 21，默认 7。21 天需要生效中的会员，否则自动降级为 7 */
    private Integer planDays;

}
