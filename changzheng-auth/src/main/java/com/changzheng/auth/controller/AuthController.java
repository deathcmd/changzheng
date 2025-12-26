package com.changzheng.auth.controller;

import com.changzheng.auth.dto.BindStudentRequest;
import com.changzheng.auth.dto.BindStudentResponse;
import com.changzheng.auth.dto.LoginResponse;
import com.changzheng.auth.dto.UpdateUserProfileRequest;
import com.changzheng.auth.dto.WxLoginRequest;
import com.changzheng.auth.service.AuthService;
import com.changzheng.common.entity.User;
import com.changzheng.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "微信小程序登录")
    @PostMapping("/wx/login")
    public R<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        LoginResponse response = authService.wxLogin(request);
        return R.ok(response);
    }

    @Operation(summary = "学生身份认证")
    @PostMapping("/bindStudent")
    public R<BindStudentResponse> bindStudent(@RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody BindStudentRequest request) {
        BindStudentResponse response = authService.bindStudent(userId, request);
        return R.ok("认证成功", response);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<LoginResponse> refreshToken(@RequestBody RefreshRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return R.ok(response);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userInfo")
    public R<User> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        User user = authService.getUserInfo(userId);
        return R.ok(user);
    }

    @Operation(summary = "更新用户资料(头像昵称)")
    @PostMapping("/profile")
    public R<String> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody UpdateUserProfileRequest request) {
        authService.updateUserProfile(userId, request);
        return R.ok("更新成功", null);
    }

    @lombok.Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}
