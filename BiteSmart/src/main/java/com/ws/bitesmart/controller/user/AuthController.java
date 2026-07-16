package com.ws.bitesmart.controller.user;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.constant.Constant;
import com.ws.bitesmart.dto.request.LoginRequestDTO;
import com.ws.bitesmart.dto.request.RegisterRequestDTO;
import com.ws.bitesmart.dto.request.WechatLoginRequestDTO;
import com.ws.bitesmart.dto.request.UserCredentialSetupRequestDTO;
import com.ws.bitesmart.dto.response.LoginResponseDTO;
import com.ws.bitesmart.service.user.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ws.bitesmart.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * 登录、注册、登出都走这里。
 * 路径前缀 /api/auth，在 SecurityConfig 的白名单里，不需要登录就能访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     *
     * POST /api/auth/register
     *
     * 注册成功后自动返回 Token，不需要再去登录。
     */
    @PostMapping("/register")
    public ResultVO<LoginResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        LoginResponseDTO response = authService.register(request);
        return ResultVO.success("注册成功", response);
    }

    /**
     * 用户登录
     *
     * POST /api/auth/login
     *
     * 目前支持账号密码登录，后续会加微信小程序登录。
     */
    @PostMapping("/login")
    public ResultVO<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                            HttpServletRequest servletRequest) {
        LoginResponseDTO response = authService.login(request, servletRequest);
        return ResultVO.success("登录成功", response);
    }

    /**
     * 用户登出
     *
     * POST /api/auth/logout
     *
     * 原理：把当前 Token 加入 Redis 黑名单，后续请求携带此 Token 会被拒绝。
     */
    @PostMapping("/wechat-login")
    public ResultVO<LoginResponseDTO> wechatLogin(@Valid @RequestBody WechatLoginRequestDTO request) {
        return ResultVO.success("微信登录成功", authService.wechatLogin(request));
    }

    @PostMapping("/wechat-bind")
    public ResultVO<Void> bindWechat(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody WechatLoginRequestDTO request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        authService.bindWechat(loginUser.getUserId(), request.getCode());
        return ResultVO.ok("微信绑定成功");
    }

    @PutMapping("/credentials")
    public ResultVO<Void> setupCredentials(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody UserCredentialSetupRequestDTO request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        authService.setupCredentials(loginUser.getUserId(), request);
        return ResultVO.ok("账号信息保存成功");
    }

    @PostMapping("/logout")
    public ResultVO<Void> logout(HttpServletRequest request) {
        // 从请求头中提取 Token
        String header = request.getHeader(Constant.TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(Constant.TOKEN_PREFIX)) {
            String token = header.substring(Constant.TOKEN_PREFIX.length());
            // 防止空 Token 写入 Redis
            if (!token.isEmpty()) {
                authService.logout(token);
            }
        }
        return ResultVO.ok("登出成功");
    }

}
