package com.changzheng.rank.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 个人排行榜项DTO
 * 包含专业、班级、真实姓名、微信头像
 */
@Data
public class PersonalRankDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 排名
     */
    private Integer rank;
    
    /**
     * 真实姓名（脱敏显示，如：张*三）
     */
    private String realName;
    
    /**
     * 微信昵称
     */
    private String nickName;
    
    /**
     * 微信头像URL
     */
    private String avatarUrl;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 班级名称
     */
    private String className;
    
    /**
     * 年级
     */
    private String grade;
    
    /**
     * 累计里程（公里）
     */
    private BigDecimal totalMileage;
    
    /**
     * 累计步数
     */
    private Long totalSteps;
}
