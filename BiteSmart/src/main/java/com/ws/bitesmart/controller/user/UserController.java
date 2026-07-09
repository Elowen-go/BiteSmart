package com.ws.bitesmart.controller.user;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.dto.response.LoginResponseDTO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口
 *
 * 获取当前登录用户的信息、修改个人信息等操作。
 * 需要登录后才能访问（不在 SecurityConfig 的白名单里）。
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的信息
     *
     * GET /api/users/me
     *
     * 从 Token 中解析出用户ID，然后查数据库返回完整信息。
     * @AuthenticationPrincipal 是 Spring Security 提供的注解，
     * 会自动从 SecurityContext 中取出当前登录用户（就是 JwtAuthenticationFilter 里设置的那个 LoginUser）。
     */
    @GetMapping("/me")
    public ResultVO<LoginResponseDTO.UserInfo> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ResultVO.error(401, "未登录");
        }
        SysUser user = userService.getUserById(loginUser.getUserId());
        LoginResponseDTO.UserInfo userInfo = userService.toUserInfo(user);
        return ResultVO.success(userInfo);
    }

}
