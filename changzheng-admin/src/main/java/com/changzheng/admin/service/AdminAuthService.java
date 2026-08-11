package com.changzheng.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.changzheng.admin.dto.AdminLoginDTO;
import com.changzheng.admin.dto.AdminLoginResponse;
import com.changzheng.admin.entity.Admin;
import com.changzheng.admin.mapper.AdminMapper;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import com.changzheng.common.util.RedisUtils;
import cn.hutool.crypto.SecureUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 管理员认证服务
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final int MAX_LOGIN_FAILURES = 10;
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW";

    private final AdminMapper adminMapper;
    
    private final PasswordEncoder passwordEncoder;

    private final RedisUtils redisUtils;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expire:7200000}")
    private long accessTokenExpire;

    /**
     * 管理员登录
     */
    public AdminLoginResponse login(AdminLoginDTO dto) {
        String attemptKey = loginAttemptKey(dto.getUsername());
        Long attemptNumber = redisUtils.incrementWithExpiry(attemptKey, 15, TimeUnit.MINUTES);
        if (attemptNumber == null) {
            throw new IllegalStateException("Redis did not return the login attempt count");
        }
        if (attemptNumber > MAX_LOGIN_FAILURES) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录失败次数过多，请15分钟后重试");
        }
        // 查询管理员
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, dto.getUsername())
                .eq(Admin::getStatus, 1)
        );
        
        if (admin == null) {
            passwordEncoder.matches(dto.getPassword(), DUMMY_PASSWORD_HASH);
            throw rejectedLogin();
        }
        
        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw rejectedLogin();
        }
        redisUtils.delete(attemptKey);
        
        // 更新登录时间
        admin.setLastLoginAt(LocalDateTime.now());
        adminMapper.updateById(admin);
        
        // 生成 JWT Token
        String token = generateToken(admin);
        
        // 构建响应
        return AdminLoginResponse.builder()
            .accessToken(token)
            .adminInfo(AdminLoginResponse.AdminInfo.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .nickname(admin.getRealName() != null ? admin.getRealName() : admin.getUsername())
                .role(admin.getRole())
                .build())
            .build();
    }

    /**
     * 获取管理员信息
     */
    public AdminLoginResponse.AdminInfo getAdminInfo(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "管理员不存在或已禁用");
        }
        
        return AdminLoginResponse.AdminInfo.builder()
            .id(admin.getId())
            .username(admin.getUsername())
            .nickname(admin.getRealName() != null ? admin.getRealName() : admin.getUsername())
            .role(admin.getRole())
            .build();
    }

    /**
     * 生成 JWT Token
     */
    private String generateToken(Admin admin) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
            .subject(String.valueOf(admin.getId()))
            .claim("username", admin.getUsername())
            .claim("role", admin.getRole())
            .claim("userType", "ADMIN")
            .claim("tokenType", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpire))
            .signWith(key)
            .compact();
    }

    private String loginAttemptKey(String username) {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        return "security:admin-login:" + SecureUtil.sha256(normalized);
    }

    private BusinessException rejectedLogin() {
        return new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
    }
}
