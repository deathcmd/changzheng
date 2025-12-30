package com.changzheng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 学生信息实体（用于认证的底表数据）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_student_info")
public class StudentInfo extends BaseEntity {

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别
     */
    private String gender;

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
     * 入学年份
     */
    private Integer enrollYear;

    /**
     * 学院
     */
    private String college;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 是否已绑定: 0-未绑定 1-已绑定
     */
    private Integer isBound;

    /**
     * 绑定的用户ID
     */
    private Long boundUserId;

    /**
     * 绑定时间
     */
    private LocalDateTime boundAt;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 导入批次号
     */
    private String importBatch;
}
