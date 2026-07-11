package com.ws.bitesmart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 *
 * 前后端约定：
 * - username 可以是手机号或用户名
 * - password 是明文，后端用 BCrypt 校验
 */
@Data
public class LoginRequestDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Integer roleType;

}
