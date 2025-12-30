package com.changzheng.auth.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 微信小程序服务
 */
@Slf4j
@Service
public class WxMiniAppService {

    @Value("${wx.miniapp.appid:}")
    private String appId;

    @Value("${wx.miniapp.secret:}")
    private String appSecret;

    private static final String CODE2SESSION_URL = 
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /**
     * code换取session
     */
    public WxSession code2Session(String code) {
        String url = String.format(CODE2SESSION_URL, appId, appSecret, code);
        
        try {
            String response = HttpUtil.get(url, 5000);
            log.debug("微信code2session响应: {}", response);
            
            JSONObject json = JSONUtil.parseObj(response);
            
            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                log.error("微信code2session失败: {}", response);
                throw new BusinessException(ResultCode.WX_CODE_INVALID, 
                        "微信登录失败: " + json.getStr("errmsg"));
            }
            
            WxSession session = new WxSession();
            session.setOpenid(json.getStr("openid"));
            session.setSessionKey(json.getStr("session_key"));
            session.setUnionid(json.getStr("unionid"));
            
            return session;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信code2session接口异常", e);
            throw new BusinessException(ResultCode.WX_API_ERROR, "微信接口调用失败");
        }
    }

    /**
     * 解密微信加密数据
     */
    public JSONObject decryptData(String sessionKey, String encryptedData, String iv) {
        try {
            byte[] keyBytes = Base64.decode(sessionKey);
            byte[] ivBytes = Base64.decode(iv);
            byte[] dataBytes = Base64.decode(encryptedData);
            
            AES aes = new AES("CBC", "PKCS5Padding", keyBytes, ivBytes);
            String decrypted = aes.decryptStr(dataBytes);
            
            return JSONUtil.parseObj(decrypted);
        } catch (Exception e) {
            log.error("解密微信数据失败", e);
            throw new BusinessException(ResultCode.WX_DECRYPT_ERROR);
        }
    }

    @Data
    public static class WxSession {
        private String openid;
        private String sessionKey;
        private String unionid;
    }
}
