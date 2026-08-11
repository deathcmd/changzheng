package com.changzheng.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 同步步数请求
 */
@Data
public class SyncRequest {

    @NotBlank(message = "加密数据不能为空")
    @Size(max = 65536, message = "加密数据超过长度限制")
    private String encryptedData;

    @NotBlank(message = "iv不能为空")
    @Size(max = 64, message = "iv超过长度限制")
    private String iv;
}
