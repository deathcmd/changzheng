package com.changzheng.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WxLoginRequest {

    @NotBlank(message = "code不能为空")
    private String code;

    /**
     * 微信加密数据(可选,用于获取用户信息)
     */
    private String encryptedData;

    /**
     * 加密算法初始向量
     */
    private String iv;
}
