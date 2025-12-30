package com.changzheng.auth.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.changzheng.auth.dto.BindStudentRequest;
import com.changzheng.auth.dto.BindStudentResponse;
import com.changzheng.auth.dto.LoginResponse;
import com.changzheng.auth.dto.UpdateUserProfileRequest;
import com.changzheng.auth.dto.WxLoginRequest;
import com.changzheng.auth.mapper.StudentInfoMapper;
import com.changzheng.auth.mapper.UserMapper;
import com.changzheng.common.entity.StudentInfo;
import com.changzheng.common.entity.User;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import com.changzheng.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WxMiniAppService wxMiniAppService;
    private final UserMapper userMapper;
    private final StudentInfoMapper studentInfoMapper;

    @Value("${jwt.secret:changzheng-cloud-march-secret-key-2024-very-long}")
    private String jwtSecret;

    @Value("${jwt.access-token-expire:7200000}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:604800000}")
    private Long refreshTokenExpire;

    @Value("${security.aes-key:changzheng-aes-key-2024}")
    private String aesKey;

    /**
     * 微信小程序登录
     */
    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request) {
        // 1. code换取session
        WxMiniAppService.WxSession wxSession = wxMiniAppService.code2Session(request.getCode());
        String openid = wxSession.getOpenid();
        String sessionKey = wxSession.getSessionKey();

        // 2. 查找或创建用户
        User user = userMapper.selectByOpenid(openid);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(wxSession.getUnionid());
            user.setSessionKey(encryptSessionKey(sessionKey));
            user.setNickname("微信用户");
            user.setTotalMileage(BigDecimal.ZERO);
            user.setTotalSteps(0L);
            user.setContinuousDays(0);
            user.setMaxContinuousDays(0);
            user.setRole(0);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("新用户注册: userId={}, openid={}", user.getId(), openid);
        } else {
            // 更新session_key
            user.setSessionKey(encryptSessionKey(sessionKey));
            userMapper.updateById(user);
        }

        // 3. 生成JWT
        JwtUtils jwtUtils = new JwtUtils(jwtSecret, accessTokenExpire, refreshTokenExpire);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("openid", openid);
        claims.put("nickname", user.getNickname());
        
        String accessToken = jwtUtils.generateAccessToken(user.getId(), "STUDENT", claims);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), "STUDENT");

        // 4. 构建响应
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenExpire / 1000);
        response.setNeedBind(!user.isBound());

        LoginResponse.UserInfoVO userInfo = new LoginResponse.UserInfoVO();
        userInfo.setUserId(user.getId());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setIsBound(user.isBound());
        
        // 如果已绑定学号，返回学生信息
        if (user.isBound()) {
            userInfo.setStudentNo(user.getStudentNoSuffix()); // 只返回后4位
            userInfo.setName(maskName(decryptName(user.getName()))); // 脱敏姓名
            userInfo.setClassName(user.getClassName());
            userInfo.setGrade(user.getGrade());
            userInfo.setCollege(user.getCollege());
            // 查询专业信息
            StudentInfo studentInfo = studentInfoMapper.selectByStudentNoAndName(
                    decryptStudentNo(user.getStudentNo()), decryptName(user.getName()));
            if (studentInfo != null) {
                userInfo.setMajor(studentInfo.getMajor());
            }
        }
        response.setUserInfo(userInfo);

        return response;
    }

    /**
     * 绑定学生信息（学生认证）
     */
    @Transactional
    public BindStudentResponse bindStudent(Long userId, BindStudentRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.isBound()) {
            throw new BusinessException(ResultCode.USER_ALREADY_BOUND);
        }

        // 查找学生信息底表，验证学号和姓名是否匹配
        StudentInfo studentInfo = studentInfoMapper.selectByStudentNoAndName(
                request.getStudentNo(), request.getName());
        
        if (studentInfo == null) {
            throw new BusinessException("学号或姓名不匹配，请确认信息是否正确");
        }

        // 检查该学生是否已被绑定
        if (studentInfo.getIsBound() != null && studentInfo.getIsBound() == 1) {
            throw new BusinessException(ResultCode.STUDENT_ALREADY_BOUND);
        }

        // 更新用户信息（从学生底表获取专业、班级等）
        String encryptedStudentNo = encryptStudentNo(request.getStudentNo());
        user.setStudentNo(encryptedStudentNo);
        user.setStudentNoSuffix(getStudentNoSuffix(request.getStudentNo()));
        user.setName(encryptName(request.getName()));
        user.setClassId(studentInfo.getClassName()); // 使用底表的班级
        user.setClassName(studentInfo.getClassName());
        user.setGrade(studentInfo.getGrade());
        user.setCollege(studentInfo.getCollege() != null ? studentInfo.getCollege() : "智能制造与信息工程学院");
        user.setEnrollYear(studentInfo.getEnrollYear());

        userMapper.updateById(user);

        // 更新学生底表的绑定状态（认证后不可解除）
        studentInfoMapper.updateBoundStatus(studentInfo.getId(), userId);

        log.info("用户学生认证成功: userId={}, studentNo={}, className={}", 
                userId, user.getStudentNoSuffix(), studentInfo.getClassName());

        // 返回认证信息
        BindStudentResponse response = new BindStudentResponse();
        response.setStudentNo(request.getStudentNo());
        response.setName(request.getName());
        response.setMajor(studentInfo.getMajor());
        response.setClassName(studentInfo.getClassName());
        response.setGrade(studentInfo.getGrade());
        response.setCollege(studentInfo.getCollege() != null ? studentInfo.getCollege() : "智能制造与信息工程学院");
        return response;
    }

    /**
     * 刷新Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        JwtUtils jwtUtils = new JwtUtils(jwtSecret, accessTokenExpire, refreshTokenExpire);

        try {
            // 验证refresh token
            String tokenType = jwtUtils.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                throw new BusinessException(ResultCode.TOKEN_INVALID);
            }

            Long userId = jwtUtils.getUserId(refreshToken);
            String userType = jwtUtils.getUserType(refreshToken);

            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 生成新的access token
            Map<String, Object> claims = new HashMap<>();
            claims.put("openid", user.getOpenid());
            claims.put("nickname", user.getNickname());

            String newAccessToken = jwtUtils.generateAccessToken(userId, userType, claims);

            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setExpiresIn(accessTokenExpire / 1000);

            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    /**
     * 获取当前用户信息
     */
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 清除敏感信息
        user.setSessionKey(null);
        user.setStudentNo(null);
        user.setName(null);
        return user;
    }

    /**
     * 更新用户资料(头像昵称)
     */
    @Transactional
    public void updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 更新头像和昵称
        boolean updated = false;
        if (StrUtil.isNotBlank(request.getNickName())) {
            user.setNickname(request.getNickName());
            updated = true;
        }
        if (StrUtil.isNotBlank(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
            updated = true;
        }

        if (updated) {
            userMapper.updateById(user);
            log.info("用户资料更新成功: userId={}, nickname={}", userId, user.getNickname());
        }
    }

    // ==================== 私有方法 ====================

    private String encryptSessionKey(String sessionKey) {
        if (StrUtil.isBlank(sessionKey)) return null;
        return SecureUtil.aes(aesKey.getBytes()).encryptHex(sessionKey);
    }

    private String encryptStudentNo(String studentNo) {
        return SecureUtil.aes(aesKey.getBytes()).encryptHex(studentNo);
    }

    private String encryptName(String name) {
        if (StrUtil.isBlank(name)) return null;
        return SecureUtil.aes(aesKey.getBytes()).encryptHex(name);
    }

    private String getStudentNoSuffix(String studentNo) {
        if (studentNo.length() >= 4) {
            return studentNo.substring(studentNo.length() - 4);
        }
        return studentNo;
    }

    private String decryptName(String encryptedName) {
        if (StrUtil.isBlank(encryptedName)) return "";
        try {
            return SecureUtil.aes(aesKey.getBytes()).decryptStr(encryptedName);
        } catch (Exception e) {
            log.warn("解密姓名失败");
            return "";
        }
    }

    private String decryptStudentNo(String encryptedStudentNo) {
        if (StrUtil.isBlank(encryptedStudentNo)) return "";
        try {
            return SecureUtil.aes(aesKey.getBytes()).decryptStr(encryptedStudentNo);
        } catch (Exception e) {
            log.warn("解密学号失败");
            return "";
        }
    }

    /**
     * 姓名脱敏：张三 -> 张* ，李四五 -> 李**
     */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "";
        int len = name.length();
        if (len == 1) return name;
        if (len == 2) return name.charAt(0) + "*";
        if (len == 3) return name.charAt(0) + "**";
        StringBuilder sb = new StringBuilder(name.substring(0, 2));
        for (int i = 2; i < len; i++) sb.append("*");
        return sb.toString();
    }
}
