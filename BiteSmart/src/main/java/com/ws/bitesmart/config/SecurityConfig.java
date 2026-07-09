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
 * 白名单路径（无需认证）：
 *   - /api/auth/**      → 登录、注册、刷新Token
 *   - /api/common/**    → 公共接口（如验证码、枚举查询）
 *   - /api/user/register → 用户注册
 *   - /api/user/login   → 用户登录
 *   - /swagger-ui/**    → Swagger 接口文档
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
     * 这些路径不会被 JWT 过滤器拦截认证
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/**",         // 登录、注册、刷新Token等认证接口
            "/api/common/**",       // 公共数据接口（如菜品分类、字典等）
            "/api/user/register",   // 用户注册接口
            "/api/user/login",      // 用户登录接口
            "/swagger-ui/**",       // Swagger 接口文档页面
            "/v3/api-docs/**",      // OpenAPI 文档接口
            "/webjars/**"           // webjar 静态资源
    };

    /**
     * 配置 Security 过滤器链
     *
     * 配置项说明：
     *   - csrf.disable()：禁用 CSRF 保护，因为使用 JWT Token，天然免疫 CSRF 攻击
     *   - sessionCreationPolicy(STATELESS)：不创建 HttpSession，完全无状态
     *   - authorizeHttpRequests：配置路径权限规则
     *   - exceptionHandling：配置认证失败和权限不足的处理方式
     *   - addFilterBefore：将 JWT 过滤器添加到 Spring Security 过滤器链中
     *
     * @param http HttpSecurity 构建器
     * @return 配置完成的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ===== 1. 基础安全配置 =====

                // 禁用 CSRF 保护：
                // 使用 JWT Token 认证时，Token 存放在请求头中，
                // 浏览器不会自动携带（不像 Cookie），所以 CSRF 无效。
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话策略：
                // 项目使用 JWT 认证，不创建 HttpSession，
                // 每次请求独立解析 Token，降低服务器内存占用。
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ===== 2. 路径权限配置 =====

                .authorizeHttpRequests(auth -> auth
                        // 白名单路径：无需登录即可访问
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 网站图标：无需认证
                        .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
                        // 其他所有请求：必须登录认证后才能访问
                        .anyRequest().authenticated())

                // ===== 3. 异常处理配置 =====

                .exceptionHandling(ex -> ex
                        // 401 未认证处理
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 403 权限不足处理
                        .accessDeniedHandler(accessDeniedHandler))

                // ===== 4. 过滤器注册 =====

                // 在 Spring Security 的 UsernamePasswordAuthenticationFilter 之前
                // 插入 JWT 认证过滤器，优先解析 Token 完成认证。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     *
     * 使用 BCrypt 加密算法（Spring Security 官方推荐）：
     *   - 自动加盐：每次加密结果不同
     *   - 不可逆：无法从密文反推明文
     *   - 可调节强度：默认 10 轮哈希
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
