package com.ws.bitesmart.security;

import com.alibaba.fastjson2.JSON;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证失败入口
 *
 * 当用户访问需要登录的接口但未提供有效 Token 时，
 * Spring Security 会调用此类的 commence 方法，
 * 返回统一的 401 JSON 响应，而不是重定向到登录页面。
 *
 * 触发场景：
 *   1. 请求头未携带 Authorization 字段
 *   2. Token 已过期或被篡改
 *   3. Token 在黑名单中（用户已登出）
 *
 * @author BiteSmart
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 处理认证失败请求
     *
     * @param request       导致认证失败的 HTTP 请求
     * @param response      HTTP 响应，设置 401 状态码和 JSON 响应体
     * @param authException 认证异常信息
     * @throws IOException 写入响应时可能抛出的 IO 异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("认证失败: {} {}", request.getMethod(), request.getRequestURI());

        // 设置 HTTP 状态码为 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 设置响应内容类型为 JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 设置字符编码为 UTF-8
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 构建统一的 JSON 错误响应
        ResultVO<Void> result = ResultVO.error(ResultCodeEnum.UNAUTHORIZED, "请先登录");
        // 将 ResultVO 序列化为 JSON 并写入响应体
        response.getWriter().write(JSON.toJSONString(result));
    }

}
