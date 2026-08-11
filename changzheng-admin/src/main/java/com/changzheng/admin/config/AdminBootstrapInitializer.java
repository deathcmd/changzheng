package com.changzheng.admin.config;

import com.changzheng.admin.entity.Admin;
import com.changzheng.admin.mapper.AdminMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapInitializer implements CommandLineRunner {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.username:}")
    private String username;

    @Value("${bootstrap.admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (adminMapper.selectCount(new LambdaQueryWrapper<Admin>().eq(Admin::getStatus, 1)) > 0) {
            return;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("No administrator exists. Set ADMIN_USERNAME and ADMIN_PASSWORD to bootstrap one.");
            return;
        }
        if (username.length() > 64 || password.length() < 12 || password.length() > 128) {
            throw new IllegalStateException("Bootstrap administrator credentials do not meet length requirements");
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRealName("System Administrator");
        admin.setRole("SUPER_ADMIN");
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.insert(admin);
        log.info("Bootstrapped the initial administrator account");
    }
}
