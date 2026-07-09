package com.ws.bitesmart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册请求参数
 *
 * 注意：
 * - roleType 如果不传，默认注册为普通用户（10）
 * - 商家注册走专门的商家入驻流程，不走这个接口
 */
@Data
public class RegisterRequestDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 昵称，非必填 */
    private String nickname;

    /** 手机号，非必填 */
    private String phone;

    /**
     * 角色类型：10-普通用户 20-商家 30-配送员 40-管理员
     * 前端不传时默认注册为普通用户
     */
    private Integer roleType;

}
