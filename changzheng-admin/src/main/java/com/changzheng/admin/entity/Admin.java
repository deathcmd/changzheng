package com.changzheng.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体
 */
@Data
@TableName("t_admin")
public class Admin {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String password;
    
    private String realName;
    
    private String phone;
    
    private String email;
    
    private String role;
    
    private Integer status;
    
    private LocalDateTime lastLoginAt;
    
    private String lastLoginIp;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
