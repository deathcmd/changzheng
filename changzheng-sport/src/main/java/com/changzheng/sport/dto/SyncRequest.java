package com.changzheng.sport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 同步步数请求
 */
@Data
public class SyncRequest {

    @NotBlank(message = "加密数据不能为空")
    private String encryptedData;

    @NotBlank(message = "iv不能为空")
    private String iv;
}
