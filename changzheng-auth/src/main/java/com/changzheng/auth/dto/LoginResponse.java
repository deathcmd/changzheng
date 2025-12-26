package com.changzheng.auth.dto;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Boolean needBind;
    private UserInfoVO userInfo;

    @Data
    public static class UserInfoVO {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private Boolean isBound;
        // 学生认证信息
        private String studentNo;
        private String name;
        private String major;
        private String className;
        private String grade;
        private String college;
    }
}
