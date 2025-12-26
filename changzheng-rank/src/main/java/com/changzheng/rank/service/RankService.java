package com.changzheng.rank.service;

import com.changzheng.common.entity.User;
import com.changzheng.rank.dto.*;
import com.changzheng.rank.mapper.RankMapper;
import com.changzheng.rank.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final StringRedisTemplate redisTemplate;

    private static final String RANK_CACHE_KEY = "rank:personal:cache";
    private static final int CACHE_EXPIRE_MINUTES = 5;

    /**
     * 获取个人排行榜
     */
    public Map<String, Object> getPersonalRank(String grade, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PersonalRankDTO> list = rankMapper.selectPersonalRank(grade, offset, pageSize);
        int total = rankMapper.countPersonalRank(grade);
        
        // 设置排名并脱敏真实姓名
        int rank = offset + 1;
        for (PersonalRankDTO dto : list) {
            dto.setRank(rank++);
            dto.setRealName(maskName(dto.getRealName()));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return result;
    }

    /**
     * 获取班级排行榜
     */
    public Map<String, Object> getClassRank(String grade, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ClassRankDTO> list = rankMapper.selectClassRank(grade, offset, pageSize);
        int total = rankMapper.countClassRank(grade);
        
        // 设置排名
        int rank = offset + 1;
        for (ClassRankDTO dto : list) {
            dto.setRank(rank++);
            // 保留2位小数
            if (dto.getAvgMileage() != null) {
                dto.setAvgMileage(dto.getAvgMileage().setScale(2, RoundingMode.HALF_UP));
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return result;
    }

    /**
     * 获取年级排行榜
     */
    public Map<String, Object> getGradeRank(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<GradeRankDTO> list = rankMapper.selectGradeRank(offset, pageSize);
        int total = rankMapper.countGradeRank();
        
        // 设置排名
        int rank = offset + 1;
        for (GradeRankDTO dto : list) {
            dto.setRank(rank++);
            if (dto.getAvgMileage() != null) {
                dto.setAvgMileage(dto.getAvgMileage().setScale(2, RoundingMode.HALF_UP));
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return result;
    }

    /**
     * 获取我的排名（使用学生认证信息）
     */
    public MyRankDTO getMyRank(Long userId, String type) {
        // 查询用户学生认证信息
        Map<String, Object> studentInfo = userMapper.selectUserStudentInfo(userId);
        if (studentInfo == null || studentInfo.isEmpty()) {
            return null;
        }
        
        MyRankDTO dto = new MyRankDTO();
        
        // 使用学生认证信息
        String realName = (String) studentInfo.get("realName");
        dto.setRealName(maskName(realName));
        dto.setMajor((String) studentInfo.get("major"));
        dto.setClassName((String) studentInfo.get("className"));
        dto.setAvatarUrl((String) studentInfo.get("avatarUrl"));
        
        // 获取里程
        Object mileage = studentInfo.get("totalMileage");
        if (mileage != null) {
            dto.setTotalMileage(new BigDecimal(mileage.toString()));
        } else {
            dto.setTotalMileage(BigDecimal.ZERO);
        }
        
        // 查询排名
        Integer rank = rankMapper.selectUserRank(userId);
        dto.setRank(rank != null ? rank : 0);
        
        return dto;
    }

    /**
     * 姓名脱敏处理
     * 张三 -> 张*
     * 张三丰 -> 张*丰
     * 欧阳娜娜 -> 欧**娜
     */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        int len = name.length();
        if (len == 1) {
            return name;
        } else if (len == 2) {
            return name.charAt(0) + "*";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(name.charAt(0));
            for (int i = 1; i < len - 1; i++) {
                sb.append("*");
            }
            sb.append(name.charAt(len - 1));
            return sb.toString();
        }
    }
}
