package com.ws.bitesmart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应数据
 *
 * 返回给前端的登录结果，包含：
 * - token：后续请求的凭证
 * - user：用户基本信息（密码不会返回）
 */
@Data
@Builder
@AllArgsConstructor
public class LoginResponseDTO {

    /** JWT Token，后续请求放在 Authorization 头里 */
    private String token;

    /** Token 类型，固定为 "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Token 过期时间（时间戳） */
    private long expireTime;

    /** 用户基本信息 */
    private UserInfo user;

    /**
     * 返回给前端的用户信息（不含密码敏感字段）
     */
    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private Integer roleType;
    }

}
