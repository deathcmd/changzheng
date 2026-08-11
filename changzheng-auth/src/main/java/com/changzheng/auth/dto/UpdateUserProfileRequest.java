package com.changzheng.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户资料请求
 */
@Data
public class UpdateUserProfileRequest {

    /**
     * 昵称
     */
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickName;

    /**
     * 头像URL
     */
    @Size(max = 2048, message = "头像地址长度超出限制")
    @Pattern(regexp = "^https://[^\s]+$", message = "头像地址必须使用HTTPS")
    private String avatarUrl;
}
