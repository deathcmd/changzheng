package com.changzheng.rank.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.changzheng.common.entity.User;
import com.changzheng.rank.dto.*;
import com.changzheng.rank.mapper.RankMapper;
import com.changzheng.rank.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排行榜服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private final RankMapper rankMapper;
    private final UserMapper userMapper;
    
    @Value("${security.aes-key:changzheng2024ab}")
    private String aesKey;

    /**
     * 获取总榜（个人排行榜）
     */
    public Map<String, Object> getTotalRank(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PersonalRankDTO> list = rankMapper.selectTotalRank(offset, pageSize);
        int total = rankMapper.countTotalRank();
        
        // 设置排名并脱敏真实姓名
        int rank = offset + 1;
        for (PersonalRankDTO dto : list) {
            dto.setRank(rank++);
            dto.setRealName(maskName(decryptName(dto.getRealName())));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return result;
    }

    /**
     * 获取年级榜（用户在自己年级的排名）
     */
    public Map<String, Object> getGradeRank(Long userId, int page, int pageSize) {
        // 获取用户年级
        User user = userMapper.selectById(userId);
        if (user == null || user.getGrade() == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("records", new ArrayList<>());
            result.put("total", 0);
            result.put("userGrade", null);
            return result;
        }
        
        String grade = user.getGrade();
        int offset = (page - 1) * pageSize;
        List<PersonalRankDTO> list = rankMapper.selectRankByGrade(grade, offset, pageSize);
        int total = rankMapper.countRankByGrade(grade);
        
        // 设置排名并脱敏真实姓名
        int rank = offset + 1;
        for (PersonalRankDTO dto : list) {
            dto.setRank(rank++);
            dto.setRealName(maskName(decryptName(dto.getRealName())));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("userGrade", grade);
        return result;
    }

    /**
     * 获取我的排名
     */
    public MyRankDTO getMyRank(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        
        MyRankDTO dto = new MyRankDTO();
        dto.setRealName(maskName(decryptName(user.getName())));
        dto.setClassName(user.getClassName());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setTotalMileage(user.getTotalMileage() != null ? user.getTotalMileage() : BigDecimal.ZERO);
        
        // 查询排名
        Integer rank = rankMapper.selectUserRank(userId);
        dto.setRank(rank != null ? rank : 0);
        
        return dto;
    }

    /**
     * 解密姓名
     */
    private String decryptName(String encryptedName) {
        if (StrUtil.isBlank(encryptedName)) {
            return "";
        }
        try {
            return SecureUtil.aes(aesKey.getBytes()).decryptStr(encryptedName);
        } catch (Exception e) {
            log.warn("解密姓名失败: {}", encryptedName);
            return encryptedName;
        }
    }

    /**
     * 姓名脱敏处理：保留姓，隐藏名字
     * 张三 -> 张*
     * 李四五 -> 李**
     * 欧阳娜娜 -> 欧阳**
     */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        int len = name.length();
        if (len == 1) {
            return name;
        } else if (len == 2) {
            // 2个字：保留第1个字，隐藏第2个
            return name.charAt(0) + "*";
        } else if (len == 3) {
            // 3个字：保留第1个字，隐藏后2个
            return name.charAt(0) + "**";
        } else {
            // 4个字及以上（可能是复姓）：保留前2个字，隐藏其余
            StringBuilder sb = new StringBuilder();
            sb.append(name.substring(0, 2));
            for (int i = 2; i < len; i++) {
                sb.append("*");
            }
            return sb.toString();
        }
    }
}
