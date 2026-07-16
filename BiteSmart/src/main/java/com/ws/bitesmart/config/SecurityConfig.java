package com.ws.bitesmart.config;

import com.ws.bitesmart.security.AccessDeniedHandlerImpl;
import com.ws.bitesmart.security.JwtAuthenticationEntryPoint;
import com.ws.bitesmart.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 *
 * 职责：
 *   1. 配置 HTTP 安全策略（CSRF、Session、路径权限）
 *   2. 注册 JWT 认证过滤器
 *   3. 配置 401/403 异常处理器
 *   4. 声明密码编码器（BCrypt）
 *
 * 认证流程：
 *   请求 → CorsFilter → JwtAuthenticationFilter（解析Token）→ SecurityFilterChain（权限判断）→ Controller
 *                                                              ↓ 未认证 → JwtAuthenticationEntryPoint（401）
 *                                                              ↓ 无权限 → AccessDeniedHandlerImpl（403）
 *
 * 权限矩阵（按路径前缀控制）：
 *   /api/auth/**      → 全部放行（白名单）
 *   /api/files/**     → 全部放行（文件访问）
 *   /api/common/**    → 全部放行（公共数据）
 *
 *   /api/users/**     → 登录用户可访问
 *   /api/dishes/**    → 登录用户可访问
 *   /api/combos/**    → 登录用户可访问
 *   /api/orders/**    → 登录用户可访问
 *   /api/health/**    → 登录用户可访问
 *
 *   /api/merchant/**  → 仅商家(20)和管理员(40)
 *   /api/admin/**     → 仅管理员(40)
 *   /api/driver/**    → 仅配送员(30)和管理员(40)
 *
 * 更细粒度的权限控制在 Controller 上使用 @PreAuthorize 注解实现。
 *
 * @author BiteSmart
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器，在每个请求前解析 Token */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 401 未认证处理器，返回统一 JSON 响应 */
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /** 403 权限不足处理器，返回统一 JSON 响应 */
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    /**
     * 无需认证即可访问的路径白名单
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/**",         // 登录、注册、刷新Token
            "/api/payment/alipay/notify", // 支付宝异步通知
            "/api/merchant/auth/**", // 商家入驻申请（普通用户也可访问）
            "/api/files/**",        // 文件访问（上传/下载/预览）
            "/uploads/**",          // 静态资源文件（图片等）
            "/api/common/**",       // 公共数据接口
            "/api/dishes/**",       // 菜品浏览（无需登录）
            "/api/combos/**",       // 套餐浏览（无需登录）
            "/api/delivery/**",     // 配送轨迹查询（无需登录）
            "/api/notices",         // 公告列表（无需登录）
            "/swagger-ui/**",       // Swagger 接口文档页面
            "/v3/api-docs/**",      // OpenAPI 文档接口
            "/webjars/**"           // webjar 静态资源
    };

    /**
     * 配置 Security 过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ===== 1. 基础安全配置 =====
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ===== 2. 路径权限配置 =====
                .authorizeHttpRequests(auth -> auth
                        // 白名单路径：无需登录
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()

                        // 管理员专属接口（仅限 ADMIN 角色）
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 商家接口（商家和管理员可访问）
                        .requestMatchers("/api/merchant/**").hasAnyRole("MERCHANT", "ADMIN")

                        // 配送员接口（配送员和管理员可访问）
                        .requestMatchers("/api/driver/**").hasAnyRole("DELIVERY_DRIVER", "ADMIN")

                        // 其他接口：登录即可访问
                        .anyRequest().authenticated())

                // ===== 3. 异常处理配置 =====
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // ===== 4. 过滤器注册 =====
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
