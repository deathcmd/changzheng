package com.changzheng.admin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员登录响应
 */
@Data
@Builder
public class AdminLoginResponse {
    
    private String accessToken;
    
    private AdminInfo adminInfo;
    
    @Data
    @Builder
    public static class AdminInfo {
        private Long id;
        private String username;
        private String nickname;
        private String role;
    }
}
