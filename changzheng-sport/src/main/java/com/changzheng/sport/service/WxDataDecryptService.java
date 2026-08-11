package com.changzheng.sport.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.changzheng.common.entity.User;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import com.changzheng.common.security.SecretValidator;
import com.changzheng.sport.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

/**
 * 微信数据解密服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxDataDecryptService {

    private final UserMapper userMapper;

    @Value("${security.aes-key}")
    private String aesKey;

    @Value("${wx.appid}")
    private String wxAppid;

    @PostConstruct
    void validateSecrets() {
        SecretValidator.requireAesKey(aesKey, "AES_KEY");
        if (wxAppid == null || wxAppid.isBlank()) {
            throw new IllegalStateException("WX_APPID must be configured");
        }
    }

    /**
     * 解密微信加密数据
     */
    public JSONObject decrypt(Long userId, String encryptedData, String iv) {
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        String encryptedSessionKey = user.getSessionKey();
        if (encryptedSessionKey == null) {
            throw new BusinessException(ResultCode.WX_SESSION_EXPIRED);
        }

        try {
            // 解密session_key
            String sessionKey = SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8))
                    .decryptStr(encryptedSessionKey);
            byte[] keyBytes = Base64.decode(sessionKey);
            byte[] ivBytes = Base64.decode(iv);
            byte[] dataBytes = Base64.decode(encryptedData);

            AES aes = new AES("CBC", "PKCS5Padding", keyBytes, ivBytes);
            String decrypted = aes.decryptStr(dataBytes);

            JSONObject payload = JSONUtil.parseObj(decrypted);
            JSONObject watermark = payload.getJSONObject("watermark");
            if (watermark == null || !wxAppid.equals(watermark.getStr("appid"))) {
                log.warn("微信数据watermark校验失败: userId={}", userId);
                throw new BusinessException(ResultCode.WX_DECRYPT_ERROR);
            }
            return payload;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解密微信数据失败", e);
            throw new BusinessException(ResultCode.WX_DECRYPT_ERROR);
        }
    }
}
