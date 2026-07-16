package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.constant.Constant;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.JwtTokenUtil;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.request.WechatLoginRequestDTO;
import com.ws.bitesmart.dto.response.LoginResponseDTO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.entity.user.UserThirdPartyIdentity;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.mapper.user.UserThirdPartyIdentityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatIdentityAuthService {

    private static final String PROVIDER = "WECHAT_MINI_PROGRAM";

    private final SysUserMapper sysUserMapper;
    private final UserThirdPartyIdentityMapper identityMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${wechat.mini-program.app-id:}")
    private String appId;

    @Value("${wechat.mini-program.app-secret:}")
    private String appSecret;

    @Transactional
    public LoginResponseDTO login(WechatLoginRequestDTO request) {
        String openId = exchangeCode(request.getCode());
        UserThirdPartyIdentity identity = identityMapper.findByProviderAndOpenId(PROVIDER, openId);
        SysUser user;
        if (identity == null) {
            if (!isUserRole(request.getRoleType())) {
                throw new BusinessException(ResultCodeEnum.ROLE_NOT_MATCH);
            }
            user = createMiniProgramUser();
            bind(user.getId(), openId);
        } else {
            user = getActiveUser(identity.getUserId());
            if (request.getRoleType() != null && !request.getRoleType().equals(user.getRoleType())) {
                throw new BusinessException(ResultCodeEnum.ROLE_NOT_MATCH);
            }
        }
        return buildLoginResponse(user, isAccountSetupRequired(user));
    }

    @Transactional
    public void bind(Long userId, WechatLoginRequestDTO request) {
        getActiveUser(userId);
        String openId = exchangeCode(request.getCode());
        UserThirdPartyIdentity existing = identityMapper.findByProviderAndOpenId(PROVIDER, openId);
        if (existing != null && !Objects.equals(existing.getUserId(), userId)) {
            throw new BusinessException(1010, "Wechat identity is already bound to another user");
        }
        if (existing == null) {
            bind(userId, openId);
        }
    }

    private SysUser createMiniProgramUser() {
        SysUser user = new SysUser();
        user.setId(SnowflakeUtil.generate());
        user.setUsername("wx_" + user.getId());
        user.setPassword(passwordEncoder.encode(String.valueOf(SnowflakeUtil.generate())));
        user.setNickname("Wechat User");
        user.setRoleType(10);
        user.setStatus(Constant.STATUS_NORMAL);
        user.setRegisterSource(20);
        sysUserMapper.insert(user);
        return user;
    }

    private void bind(Long userId, String openId) {
        UserThirdPartyIdentity identity = new UserThirdPartyIdentity();
        identity.setId(SnowflakeUtil.generate());
        identity.setUserId(userId);
        identity.setProvider(PROVIDER);
        identity.setOpenId(openId);
        try {
            identityMapper.insert(identity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(1010, "Wechat identity is already bound to another user");
        }
    }

    @SuppressWarnings("unchecked")
    private String exchangeCode(String code) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BusinessException(1008, "WeChat mini-program configuration is missing");
        }
        String url = UriComponentsBuilder
                .fromUriString("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUriString();
        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            if (response == null || response.get("openid") == null) {
                String error = response == null ? "WeChat API returned no response" : String.valueOf(response.get("errmsg"));
                throw new BusinessException(1009, error);
            }
            return String.valueOf(response.get("openid"));
        } catch (JsonProcessingException e) {
            log.warn("WeChat API returned invalid JSON", e);
            throw new BusinessException(1009, "WeChat API returned invalid response");
        } catch (RestClientException e) {
            log.warn("WeChat login API call failed", e);
            throw new BusinessException(1009, "WeChat login service is unavailable");
        }
    }

    private SysUser getActiveUser(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_NOT_FOUND);
        }
        if (Constant.STATUS_DISABLED == user.getStatus()) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED);
        }
        return user;
    }

    private LoginResponseDTO buildLoginResponse(SysUser user, boolean accountSetupRequired) {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roleType(user.getRoleType())
                .build();
        return LoginResponseDTO.builder()
                .token(jwtTokenUtil.generateToken(user.getId(), user.getRoleType()))
                .tokenType("Bearer")
                .expireTime(System.currentTimeMillis() + jwtExpiration)
                .user(userInfo)
                .accountSetupRequired(accountSetupRequired)
                .build();
    }

    private boolean isUserRole(Integer roleType) {
        return roleType == null || Integer.valueOf(10).equals(roleType);
    }

    private boolean isAccountSetupRequired(SysUser user) {
        return Integer.valueOf(10).equals(user.getRoleType())
                && user.getUsername() != null
                && user.getUsername().startsWith("wx_");
    }
}
