package com.changzheng.admin.dto;

import lombok.Data;

/**
 * 管理员登录请求
 */
@Data
public class AdminLoginDTO {
    
    private String username;
    
    private String password;
}
