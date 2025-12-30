package com.changzheng.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.changzheng.admin.dto.AdminLoginDTO;
import com.changzheng.admin.dto.AdminLoginResponse;
import com.changzheng.admin.entity.Admin;
import com.changzheng.admin.mapper.AdminMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 管理员认证服务
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminMapper adminMapper;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret:changzheng-cloud-march-secret-key-2024-very-long}")
    private String jwtSecret;

    /**
     * 管理员登录
     */
    public AdminLoginResponse login(AdminLoginDTO dto) {
        // 查询管理员
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, dto.getUsername())
                .eq(Admin::getStatus, 1)
        );
        
        if (admin == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
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
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
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
            .claim("type", "admin")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 7200000)) // 2小时
            .signWith(key)
            .compact();
    }
}
