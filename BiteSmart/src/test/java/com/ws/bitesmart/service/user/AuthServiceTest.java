package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.constant.Constant;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.JwtTokenUtil;
import com.ws.bitesmart.dto.request.LoginRequestDTO;
import com.ws.bitesmart.dto.request.UserCredentialSetupRequestDTO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.service.system.OperateLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OperateLogService operateLogService;
    @Mock private WechatIdentityAuthService wechatIdentityAuthService;
    @Mock private HttpServletRequest servletRequest;

    @Test
    void loginByPhoneAcceptsTheSelectedRole() {
        SysUser user = user(100L, "pc_user", 20);
        when(sysUserMapper.findByUsernameOrPhone("13800000000")).thenReturn(user);
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtTokenUtil.generateToken(100L, 20)).thenReturn("token");
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("13800000000");
        request.setPassword("secret");
        request.setRoleType(20);

        assertThat(service().login(request, servletRequest).getUser().getRoleType()).isEqualTo(20);
    }

    @Test
    void loginRejectsTheWrongMiniProgramRole() {
        SysUser user = user(100L, "merchant", 20);
        when(sysUserMapper.findByUsernameOrPhone("merchant")).thenReturn(user);
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("merchant");
        request.setPassword("secret");
        request.setRoleType(30);

        assertThatThrownBy(() -> service().login(request, servletRequest))
                .hasMessage(ResultCodeEnum.ROLE_NOT_MATCH.getMessage());
    }

    @Test
    void setupCredentialsReplacesWechatPlaceholderAndKeepsUserId() {
        SysUser user = user(100L, "wx_100", 10);
        when(sysUserMapper.findById(100L)).thenReturn(user);
        when(sysUserMapper.findByUsernameOrPhone("pc_user")).thenReturn(null);
        when(sysUserMapper.findByPhone("13800000000")).thenReturn(null);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        UserCredentialSetupRequestDTO request = new UserCredentialSetupRequestDTO();
        request.setUsername("pc_user");
        request.setPhone("13800000000");
        request.setPassword("secret");

        service().setupCredentials(100L, request);

        assertThat(user.getId()).isEqualTo(100L);
        assertThat(user.getUsername()).isEqualTo("pc_user");
        assertThat(user.getPhone()).isEqualTo("13800000000");
        assertThat(user.getPassword()).isEqualTo("encoded-secret");
        verify(sysUserMapper).updateCredentials(100L, "pc_user", "13800000000", "encoded-secret");
    }

    private AuthService service() {
        return new AuthService(sysUserMapper, jwtTokenUtil, redisTemplate, passwordEncoder,
                operateLogService, wechatIdentityAuthService);
    }

    private SysUser user(long id, String username, int roleType) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded");
        user.setRoleType(roleType);
        user.setStatus(Constant.STATUS_NORMAL);
        return user;
    }
}
