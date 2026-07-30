package com.ws.bitesmart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册请求参数
 *
 * 注意：
 * - roleType 如果不传，默认注册为普通用户（10）
 * - 商家可以通过这个接口创建商家账号，随后提交店铺入驻资料
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
     * 角色类型：10-普通用户 20-商家
     * 前端不传时默认注册为普通用户，配送员和管理员账号由平台创建
     */
    private Integer roleType;

}
