package com.changzheng.admin.controller;

import com.changzheng.admin.dto.AdminLoginDTO;
import com.changzheng.admin.dto.AdminLoginResponse;
import com.changzheng.admin.service.AdminAuthService;
import com.changzheng.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员登录控制器
 */
@Tag(name = "管理员认证")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public R<AdminLoginResponse> login(@Valid @RequestBody AdminLoginDTO dto) {
        try {
            AdminLoginResponse response = adminAuthService.login(dto);
            return R.ok(response);
        } catch (Exception e) {
            return R.fail("用户名或密码错误");
        }
    }

    @Operation(summary = "获取管理员信息")
    @GetMapping("/info")
    public R<AdminLoginResponse.AdminInfo> getAdminInfo(@RequestHeader("X-Admin-Id") Long adminId) {
        try {
            AdminLoginResponse.AdminInfo info = adminAuthService.getAdminInfo(adminId);
            return R.ok(info);
        } catch (Exception e) {
            return R.fail("无法获取管理员信息");
        }
    }
}
