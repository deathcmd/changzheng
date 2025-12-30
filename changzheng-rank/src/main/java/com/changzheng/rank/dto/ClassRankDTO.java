package com.changzheng.rank.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 班级排行榜项DTO
 */
@Data
public class ClassRankDTO {
    
    /**
     * 排名
     */
    private Integer rank;
    
    /**
     * 班级ID
     */
    private String classId;
    
    /**
     * 班级名称
     */
    private String className;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 年级
     */
    private String grade;
    
    /**
     * 人数
     */
    private Integer memberCount;
    
    /**
     * 班级累计里程
     */
    private BigDecimal totalMileage;
    
    /**
     * 人均里程
     */
    private BigDecimal avgMileage;
}
