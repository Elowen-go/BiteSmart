package com.ws.bitesmart.security;

import com.ws.bitesmart.common.enums.RoleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 当前登录用户信息（存于SecurityContext）
 */
@Data
@AllArgsConstructor
public class LoginUser implements AuthenticatedPrincipal {

    private Long userId;
    private Integer roleType;

    /**
     * 获取角色标识（用于权限判断）
     * ROLE_USER, ROLE_MERCHANT, ROLE_DRIVER, ROLE_ADMIN
     */
    public String getRoleName() {
        RoleTypeEnum role = RoleTypeEnum.getByCode(roleType);
        return role != null ? "ROLE_" + role.name() : "ROLE_UNKNOWN";
    }

    /**
     * 获取权限列表
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(getRoleName()));
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

}
