package com.ws.bitesmart.security;

import com.ws.bitesmart.common.constant.Constant;
import com.ws.bitesmart.common.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * 继承 OncePerRequestFilter 确保每个请求只执行一次过滤。
 * 执行流程：
 *   1. 从请求头 Authorization 中提取 Bearer Token
 *   2. 检查 Token 是否在 Redis 黑名单中（用于登出/冻结场景）
 *   3. 解析 Token 获取 userId 和 roleType
 *   4. 将用户信息设置到 SecurityContext 中供后续使用
 *
 * 注意：
 *   - 如果 Token 在黑名单中或解析失败，不会设置认证信息
 *   - 后续 SecurityConfig 中的 .anyRequest().authenticated() 会拦截未认证请求
 *   - 白名单路径（如 /api/auth/**）不会进入此过滤器的认证逻辑
 *
 * @author BiteSmart
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，用于解析 Token */
    private final JwtTokenUtil jwtTokenUtil;

    /** Redis 客户端，用于查询 Token 黑名单 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 执行请求过滤
     *
     * @param request     当前 HTTP 请求
     * @param response    当前 HTTP 响应
     * @param filterChain 过滤器链，用于传递给下一个过滤器
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 第一步：从请求头中提取 Token
        String token = extractTokenFromRequest(request);

        if (token != null) {
            // 第二步：检查 Token 是否在 Redis 黑名单中
            // 当用户主动登出或管理员冻结账号时，Token 会被加入黑名单
            // 黑名单 Key 格式：token:blacklist:{token字符串}
            String blacklistKey = Constant.REDIS_TOKEN_BLACKLIST + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.warn("Token已在黑名单中，拒绝访问");
                filterChain.doFilter(request, response);
                return;
            }

            // 第三步：解析 Token，提取用户信息
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            Integer roleType = jwtTokenUtil.getRoleTypeFromToken(token);

            if (userId != null && roleType != null) {
                // 第四步：创建认证信息并设置到 SecurityContext
                // LoginUser 是自定义的 Principal 实现，包含 userId 和 roleType
                LoginUser loginUser = new LoginUser(userId, roleType);

                // authorities 是权限列表，基于角色类型生成 ROLE_ 前缀的权限标识
                // 例如：ROLE_USER、ROLE_ADMIN 等，供 @PreAuthorize 注解使用
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                loginUser,         // principal：当前用户
                                null,              // credentials：JWT 无密码凭证
                                loginUser.getAuthorities()); // authorities：角色权限

                // 将认证信息存入 SecurityContextHolder
                // 后续在 Controller 中可通过 SecurityContextHolder.getContext().getAuthentication() 获取
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Token认证成功: userId={}, roleType={}", userId, roleType);
            } else {
                log.warn("Token解析失败: userId或roleType为空");
            }
        }

        // 第五步：无论认证成功与否，都继续执行过滤器链
        // 未认证的请求会在 SecurityConfig 的 .anyRequest().authenticated() 处被拦截
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中提取 Bearer Token
     *
     * 请求头格式：Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     *
     * @param request HTTP 请求
     * @return 去除 "Bearer " 前缀后的 Token 字符串，如果不存在或格式错误则返回 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 获取 Authorization 请求头
        String bearerToken = request.getHeader(Constant.TOKEN_HEADER);

        // 验证请求头是否存在且以 "Bearer " 开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constant.TOKEN_PREFIX)) {
            // 截取 "Bearer " 之后的部分作为 Token
            return bearerToken.substring(Constant.TOKEN_PREFIX.length());
        }
        return null;
    }

}
