package com.changzheng.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT工具类
 */
@Slf4j
public class JwtUtils {

    private static final String DEFAULT_SECRET = "changzheng-cloud-march-secret-key-2024-very-long";
    private static final long ACCESS_TOKEN_EXPIRE = 7200 * 1000L;  // 2小时
    private static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 3600 * 1000L;  // 7天

    private final SecretKey secretKey;
    private final long accessTokenExpire;
    private final long refreshTokenExpire;

    public JwtUtils() {
        this(DEFAULT_SECRET, ACCESS_TOKEN_EXPIRE, REFRESH_TOKEN_EXPIRE);
    }

    public JwtUtils(String secret, long accessTokenExpire, long refreshTokenExpire) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpire = accessTokenExpire;
        this.refreshTokenExpire = refreshTokenExpire;
    }

    /**
     * 生成AccessToken
     */
    public String generateAccessToken(Long userId, String userType, Map<String, Object> claims) {
        return generateToken(userId, userType, claims, accessTokenExpire, "access");
    }

    /**
     * 生成RefreshToken
     */
    public String generateRefreshToken(Long userId, String userType) {
        return generateToken(userId, userType, null, refreshTokenExpire, "refresh");
    }

    private String generateToken(Long userId, String userType, Map<String, Object> claims, 
                                  long expireTime, String tokenType) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);
        
        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(String.valueOf(userId))
                .claim("userType", userType)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(secretKey);
        
        if (claims != null && !claims.isEmpty()) {
            claims.forEach(builder::claim);
        }
        
        return builder.compact();
    }

    /**
     * 解析Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Token解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从Token获取用户类型
     */
    public String getUserType(String token) {
        Claims claims = parseToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * 获取Token类型 (access/refresh)
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenType", String.class);
    }

    /**
     * 获取Token ID (用于黑名单)
     */
    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * 获取过期时间
     */
    public Date getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * 判断Token是否即将过期(15分钟内)
     */
    public boolean isTokenAboutToExpire(String token) {
        Date expiration = getExpiration(token);
        long diff = expiration.getTime() - System.currentTimeMillis();
        return diff < 15 * 60 * 1000L;
    }

    public long getAccessTokenExpire() {
        return accessTokenExpire;
    }

    public long getRefreshTokenExpire() {
        return refreshTokenExpire;
    }
}
