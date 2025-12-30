package com.changzheng.rank.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 年级排行榜项DTO
 */
@Data
public class GradeRankDTO {
    
    /**
     * 排名
     */
    private Integer rank;
    
    /**
     * 年级（如2022级）
     */
    private String grade;
    
    /**
     * 入学年份
     */
    private Integer enrollYear;
    
    /**
     * 人数
     */
    private Integer memberCount;
    
    /**
     * 年级累计里程
     */
    private BigDecimal totalMileage;
    
    /**
     * 人均里程
     */
    private BigDecimal avgMileage;
}
