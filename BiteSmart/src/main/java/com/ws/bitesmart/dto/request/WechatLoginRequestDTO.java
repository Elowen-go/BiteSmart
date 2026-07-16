package com.ws.bitesmart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginRequestDTO {

    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    /** 小程序登录角色：10普通用户、20商家、30配送员。 */
    private Integer roleType;
}
