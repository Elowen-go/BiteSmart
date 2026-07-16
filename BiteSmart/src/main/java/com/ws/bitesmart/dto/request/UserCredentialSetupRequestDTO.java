package com.ws.bitesmart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 微信首次登录后完善 PC 账号的请求参数。 */
@Data
public class UserCredentialSetupRequestDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
