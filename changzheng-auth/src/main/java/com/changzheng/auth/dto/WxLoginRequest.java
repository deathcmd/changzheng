package com.changzheng.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WxLoginRequest {

    @NotBlank(message = "code不能为空")
    @Size(max = 256, message = "code长度不能超过256个字符")
    private String code;

    /**
     * 微信加密数据(可选,用于获取用户信息)
     */
    @Size(max = 8192, message = "encryptedData长度超出限制")
    private String encryptedData;

    /**
     * 加密算法初始向量
     */
    @Size(max = 256, message = "iv长度超出限制")
    private String iv;
}
