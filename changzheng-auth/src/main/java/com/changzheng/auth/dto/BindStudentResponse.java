package com.changzheng.auth.dto;

import lombok.Data;

/**
 * 学生认证响应
 */
@Data
public class BindStudentResponse {

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String name;

    /**
     * 专业
     */
    private String major;

    /**
     * 班级
     */
    private String className;

    /**
     * 年级
     */
    private String grade;

    /**
     * 学院
     */
    private String college;
}
