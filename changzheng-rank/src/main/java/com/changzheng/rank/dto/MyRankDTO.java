package com.changzheng.rank.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 我的排名DTO
 */
@Data
public class MyRankDTO {
    
    /**
     * 我的排名
     */
    private Integer rank;
    
    /**
     * 累计里程
     */
    private BigDecimal totalMileage;
    
    /**
     * 真实姓名（脱敏）
     */
    private String realName;
    
    /**
     * 微信头像
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
}
