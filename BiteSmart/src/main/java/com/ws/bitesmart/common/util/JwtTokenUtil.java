package com.ws.bitesmart.common.util;

import com.ws.bitesmart.common.constant.Constant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT（JSON Web Token）工具类
 *
 * 负责 Token 的生成、解析和验证。
 * 将用户ID和角色类型编码到 Token 中，实现无状态认证。
 * Token 存储在客户端，服务端不保存 Session，降低服务器内存压力。
 *
 * 使用方式：
 *   1. 登录成功后调用 generateToken() 生成 Token 返回给客户端
 *   2. 客户端每次请求在 Authorization 头携带 "Bearer {token}"
 *   3. JwtAuthenticationFilter 拦截请求并调用本类方法解析验证
 *
 * @author BiteSmart
 */
@Slf4j
@Component
public class JwtTokenUtil {

    /** HMAC-SHA 加密密钥，用于签名和验证 Token */
    private final SecretKey secretKey;

    /** Token 过期时间（毫秒），默认24小时，从 application.yml 读取 */
    private final long expiration;

    /** 刷新 Token 过期时间（毫秒），默认30天，用于无感续签 */
    private final long refreshExpiration;

    /**
     * 构造方法，通过 Spring 注入配置值
     *
     * @param secret             JWT 签名密钥字符串
     * @param expiration         Token 过期时间（毫秒）
     * @param refreshExpiration  刷新 Token 过期时间（毫秒）
     */
    public JwtTokenUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        // 将密钥字符串转为 HMAC-SHA 所需的 SecretKey 对象
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * 生成访问令牌（Access Token）
     *
     * Token Payload 结构：
     *   - userId:   用户唯一ID（Long）
     *   - roleType: 用户角色类型（Integer，10/20/30/40）
     *   - iat:      签发时间
     *   - exp:      过期时间
     *
     * @param userId   用户ID（雪花算法生成）
     * @param roleType 角色类型（10-用户 20-商家 30-配送员 40-管理员）
     * @return 签名的 JWT 字符串
     */
    public String generateToken(Long userId, Integer roleType) {
        Date now = new Date();
        String token = Jwts.builder()
                .claim(Constant.CLAIM_USER_ID, userId)      // 存入用户ID
                .claim(Constant.CLAIM_ROLE_TYPE, roleType)  // 存入角色类型
                .issuedAt(now)                               // 签发时间
                .expiration(new Date(now.getTime() + expiration)) // 过期时间
                .signWith(secretKey)                         // 使用 HMAC-SHA 签名
                .compact();
        log.debug("生成Token: userId={}, roleType={}, 有效期={}ms", userId, roleType, expiration);
        return token;
    }

    /**
     * 生成刷新令牌（Refresh Token）
     * 用于 Access Token 过期后无感续签，有效期比 Access Token 长
     *
     * @param userId 用户ID
     * @return 签名的刷新 Token 字符串
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .claim(Constant.CLAIM_USER_ID, userId)
                .claim("type", "refresh")  // 标记为 refresh token，与普通 token 区分
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token，提取 Claims（载荷数据）
     *
     * 解析失败场景：
     *   1. Token 过期 → 返回 null，前端应使用 refresh token 续签
     *   2. Token 被篡改 → 签名验证失败，返回 null
     *   3. Token 格式错误 → 解析异常，返回 null
     *
     * @param token JWT 字符串
     * @return Claims 对象（包含 userId、roleType 等），解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            // 验证签名并解析 Token
            return Jwts.parser()
                    .verifyWith(secretKey)            // 使用密钥验证签名
                    .build()
                    .parseSignedClaims(token)         // 解析 Token 载荷
                    .getPayload();                    // 提取 Claims
        } catch (ExpiredJwtException e) {
            // Token 已过期（属于正常业务场景，记录警告即可）
            log.warn("Token已过期");
            return null;
        } catch (MalformedJwtException e) {
            // Token 格式错误（可能是伪造的 Token）
            log.warn("Token格式异常: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            // 不支持的 Token 类型
            log.warn("不支持的Token类型: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            // Token 字符串为空或格式不合法
            log.warn("Token参数非法: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID，解析失败返回 null
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(Constant.CLAIM_USER_ID, Long.class) : null;
    }

    /**
     * 从 Token 中提取角色类型
     *
     * @param token JWT 字符串
     * @return 角色类型编码（10/20/30/40），解析失败返回 null
     */
    public Integer getRoleTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(Constant.CLAIM_ROLE_TYPE, Integer.class) : null;
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     *
     * @param token JWT 字符串
     * @return true=有效, false=无效或已过期
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 判断 Token 是否已过期
     * 与 parseToken 不同，此方法通过捕获 ExpiredJwtException 来判断，
     * 适用于需要明确区分"过期"和"其他错误"的场景
     *
     * @param token JWT 字符串
     * @return true=已过期, false=未过期或无法解析
     */
    public boolean isTokenExpired(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;  // 明确识别为过期
        } catch (Exception e) {
            // 其他解析异常也视为无效
            return true;
        }
    }

}
