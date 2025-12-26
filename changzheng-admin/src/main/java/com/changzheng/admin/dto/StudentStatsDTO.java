package com.changzheng.admin.dto;

import lombok.Data;

/**
 * 学生统计DTO
 */
@Data
public class StudentStatsDTO {

    /**
     * 总人数
     */
    private Integer total;

    /**
     * 已绑定数
     */
    private Integer bound;

    /**
     * 未绑定数
     */
    private Integer unbound;

    /**
     * 绑定率
     */
    private Double bindRate;
}
