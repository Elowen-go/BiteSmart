package com.ws.bitesmart.security;

import com.alibaba.fastjson2.JSON;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 权限不足处理器
 *
 * 当用户已登录（Token 有效），但没有当前接口所需权限时触发。
 * 与 JwtAuthenticationEntryPoint 的区别：
 *   - JwtAuthenticationEntryPoint：未登录（401）
 *   - AccessDeniedHandlerImpl：已登录但权限不足（403）
 *
 * 触发场景：
 *   1. 普通用户访问管理员接口
 *   2. 商家访问其他商家的数据接口
 *   3. 使用 @PreAuthorize 注解但角色不匹配
 *
 * @author BiteSmart
 */
@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    /**
     * 处理权限不足的请求
     *
     * @param request                HTTP 请求
     * @param response               HTTP 响应
     * @param accessDeniedException  权限拒绝异常，包含详细的错误信息
     * @throws IOException 写入响应时可能抛出的 IO 异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("权限不足: {} {} - {}",
                request.getMethod(), request.getRequestURI(),
                accessDeniedException.getMessage());

        // 设置 HTTP 状态码为 403 Forbidden
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        // 设置响应内容类型为 JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 构建统一的 JSON 错误响应
        ResultVO<Void> result = ResultVO.error(ResultCodeEnum.FORBIDDEN, "权限不足");
        response.getWriter().write(JSON.toJSONString(result));
    }

}
