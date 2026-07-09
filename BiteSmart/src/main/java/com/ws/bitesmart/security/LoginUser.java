package com.ws.bitesmart.security;

import com.ws.bitesmart.common.enums.RoleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 当前登录用户信息
 *
 * 实现了 Spring Security 的 AuthenticatedPrincipal 接口，
 * 在 JwtAuthenticationFilter 中解析 Token 后创建此对象，
 * 并存入 SecurityContextHolder 供后续请求处理使用。
 *
 * 在 Controller 中获取当前用户信息的方式：
 *   // 方式一：从 SecurityContext 获取
 *   LoginUser loginUser = (LoginUser) SecurityContextHolder
 *       .getContext().getAuthentication().getPrincipal();
 *
 *   // 方式二：自定义注解 + 参数解析器（后续可扩展）
 *
 * @author BiteSmart
 */
@Data
@AllArgsConstructor
public class LoginUser implements AuthenticatedPrincipal {

    /** 用户 ID（雪花算法生成） */
    private Long userId;

    /** 角色类型编码：10-普通用户 20-商家 30-配送员 40-管理员 */
    private Integer roleType;

    /**
     * 获取 Spring Security 角色标识
     *
     * 角色格式为 "ROLE_用户端名称"，如：
     *   - ROLE_USER    → 普通用户
     *   - ROLE_MERCHANT → 商家
     *   - ROLE_DRIVER  → 配送员
     *   - ROLE_ADMIN   → 管理员
     *
     * 此标识用于 @PreAuthorize("hasRole('ADMIN')") 等注解的权限判断
     *
     * @return 角色标识字符串
     */
    public String getRoleName() {
        // 根据编码查询对应的角色枚举
        RoleTypeEnum role = RoleTypeEnum.getByCode(roleType);
        // 如果找到对应枚举，返回 "ROLE_枚举名"；否则返回 "ROLE_UNKNOWN"
        return role != null ? "ROLE_" + role.name() : "ROLE_UNKNOWN";
    }

    /**
     * 获取当前用户拥有的所有权限集合
     *
     * 目前基于角色类型生成一个简单的权限标识（如 ROLE_ADMIN），
     * 后续如果需要更细粒度的权限控制，可以从数据库查询用户的权限列表。
     *
     * @return GrantedAuthority 集合，Spring Security 用于鉴权
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 创建一个权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // 添加基于角色的权限标识
        authorities.add(new SimpleGrantedAuthority(getRoleName()));
        return authorities;
    }

    /**
     * 实现 AuthenticatedPrincipal 接口的 getName 方法
     * 返回用户 ID 的字符串形式，用于标识当前认证用户
     *
     * @return 用户 ID 字符串
     */
    @Override
    public String getName() {
        return String.valueOf(userId);
    }

}
