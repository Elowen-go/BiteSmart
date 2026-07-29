package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.constant.Constant;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.JwtTokenUtil;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.request.LoginRequestDTO;
import com.ws.bitesmart.dto.request.RegisterRequestDTO;
import com.ws.bitesmart.dto.request.WechatLoginRequestDTO;
import com.ws.bitesmart.dto.request.UserCredentialSetupRequestDTO;
import com.ws.bitesmart.dto.response.LoginResponseDTO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.service.system.OperateLogService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 *
 * 负责注册、登录、登出、Token刷新这些跟认证相关的逻辑。
 * 密码用 BCrypt 加密存储，不会保存明文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OperateLogService operateLogService;
    private final WechatIdentityAuthService wechatIdentityAuthService;

    /** Token 过期时间（毫秒），从 yml 读取，与 JwtTokenUtil 保持一致 */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 用户注册
     *
     * 流程：
     * 1. 检查用户名是否已被注册
     * 2. 密码 BCrypt 加密
     * 3. 生成雪花ID，写入数据库
     * 4. 自动生成 Token 并返回
     */
    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO request) {
        // 1. 检查用户名唯一性（业务层先查一次）
        SysUser existUser = sysUserMapper.findByUsernameOrPhone(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }

        // 2. 密码加密
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 构建用户对象
        SysUser user = new SysUser();
        user.setId(SnowflakeUtil.generate());
        user.setUsername(request.getUsername());
        user.setPassword(encodedPassword);
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setRoleType(request.getRoleType() != null ? request.getRoleType() : 10);
        user.setStatus(Constant.STATUS_NORMAL);
        user.setRegisterSource(10); // PC端注册

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 高并发下两个同时注册同名用户，数据库唯一索引兜底
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }
        log.info("新用户注册成功: userId={}, username={}, roleType={}", user.getId(), user.getUsername(), user.getRoleType());

        operateLogService.record(user.getId(), user.getUsername(), user.getRoleType(),
                "用户注册", null, null, null, null, null, null);

        // 4. 生成 Token 并返回
        return buildLoginResponse(user);
    }

    /**
     * 用户登录
     *
     * 流程：
     * 1. 根据用户名查用户
     * 2. 校验密码
     * 3. 检查账号状态
     * 4. 更新最后登录时间和IP
     * 5. 生成 Token 返回
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request, HttpServletRequest servletRequest) {
        // 1. 查询用户
        SysUser user = sysUserMapper.findByUsernameOrPhone(request.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_NOT_FOUND);
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_ERROR);
        }

        // 3. 检查账号状态
        if (Constant.STATUS_DISABLED == user.getStatus()) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED);
        }

        if (user.getUsername() != null && user.getUsername().startsWith("wx_")) {
            throw new BusinessException(1008, "请先在小程序完善账号信息");
        }

        // 4. 校验角色类型
        if (request.getRoleType() != null && !request.getRoleType().equals(user.getRoleType())) {
            throw new BusinessException(ResultCodeEnum.ROLE_NOT_MATCH);
        }

        // 5. 更新登录时间
        String clientIp = getClientIp(servletRequest);
        sysUserMapper.updateLoginTime(user.getId(), LocalDateTime.now(), clientIp);

        log.info("用户登录成功: userId={}, username={}, ip={}", user.getId(), user.getUsername(), clientIp);

        operateLogService.record(user.getId(), user.getUsername(), user.getRoleType(),
                "用户登录", null, null, null, clientIp, null, null);

        // 6. 生成 Token
        return buildLoginResponse(user);
    }

    /**
     * 用户登出
     *
     * 把 Token 加入 Redis 黑名单，使当前 Token 立即失效。
     * 黑名单过期时间 = Token 剩余有效期，到期自动清理。
     */
    @Transactional
    public LoginResponseDTO wechatLogin(WechatLoginRequestDTO request) {
        return wechatIdentityAuthService.login(request);
    }

    @Transactional
    public void bindWechat(Long userId, String code) {
        WechatLoginRequestDTO request = new WechatLoginRequestDTO();
        request.setCode(code);
        wechatIdentityAuthService.bind(userId, request);
    }

    public boolean isWechatBound(Long userId) {
        return wechatIdentityAuthService.isBound(userId);
    }

    /** 完善微信临时账号，使同一用户可以使用 PC 账号密码登录。 */
    @Transactional
    public void setupCredentials(Long userId, UserCredentialSetupRequestDTO request) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_NOT_FOUND);
        }
        if (!Integer.valueOf(10).equals(user.getRoleType())
                || user.getUsername() == null
                || !user.getUsername().startsWith("wx_")) {
            throw new BusinessException(1008, "当前账号不需要重复完善");
        }
        SysUser usernameOrPhone = sysUserMapper.findByUsernameOrPhone(request.getUsername());
        if (usernameOrPhone != null && !userId.equals(usernameOrPhone.getId())) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }
        SysUser phoneUser = sysUserMapper.findByPhone(request.getPhone());
        if (phoneUser != null && !userId.equals(phoneUser.getId())) {
            throw new BusinessException(1001, "手机号已被其他账号使用");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        sysUserMapper.updateCredentials(userId, request.getUsername(), request.getPhone(), encodedPassword);
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(encodedPassword);
    }

    public void logout(String token) {
        // 解析 Token，计算剩余有效时间作为黑名单过期时间
        long ttl = jwtExpiration; // 默认24小时
        Claims claims = jwtTokenUtil.parseToken(token);
        if (claims != null && claims.getExpiration() != null) {
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0) {
                ttl = remaining;
            }
        }

        String blacklistKey = Constant.REDIS_TOKEN_BLACKLIST + token;
        redisTemplate.opsForValue().set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
        log.info("用户登出，Token已加入黑名单，剩余有效={}ms", ttl);
    }

    /**
     * 构建登录成功响应
     *
     * 组装 Token 和用户信息返回给前端。
     * 注意：密码字段不会返回。
     */
    private LoginResponseDTO buildLoginResponse(SysUser user) {
        // 生成 Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getRoleType());

        // 过期时间从 yml 配置读取，保证与 JwtTokenUtil 一致
        long expireTime = System.currentTimeMillis() + jwtExpiration;

        // 组装用户信息（不含密码）
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roleType(user.getRoleType())
                .build();

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expireTime(expireTime)
                .user(userInfo)
                .build();
    }

    /**
     * 从请求中获取客户端真实 IP
     *
     * 如果经过反向代理（Nginx），从 X-Forwarded-For 取真实IP；
     * 否则直接从 getRemoteAddr 取。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能有多级代理，取第一个就是真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
