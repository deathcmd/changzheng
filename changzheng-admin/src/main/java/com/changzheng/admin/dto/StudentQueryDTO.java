package com.changzheng.admin.dto;

import lombok.Data;

/**
 * 学生查询DTO
 */
@Data
public class StudentQueryDTO {

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer size = 20;

    /**
     * 关键词（学号/姓名）
     */
    private String keyword;

    /**
     * 专业
     */
    private String major;

    /**
     * 班级
     */
    private String className;

    /**
     * 绑定状态：0-未绑定 1-已绑定
     */
    private Integer isBound;
}
