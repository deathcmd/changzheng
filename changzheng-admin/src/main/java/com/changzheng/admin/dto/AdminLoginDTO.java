package com.changzheng.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录请求
 */
@Data
public class AdminLoginDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名过长")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码过长")
    private String password;
}
