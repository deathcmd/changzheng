package com.changzheng.auth.dto;

import lombok.Data;

/**
 * 更新用户资料请求
 */
@Data
public class UpdateUserProfileRequest {

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像URL
     */
    private String avatarUrl;
}
