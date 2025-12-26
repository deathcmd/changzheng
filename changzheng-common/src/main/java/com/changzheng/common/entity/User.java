package com.changzheng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /**
     * 微信小程序openid
     */
    private String openid;

    /**
     * 微信unionid
     */
    private String unionid;

    /**
     * 微信session_key(加密存储)
     */
    private String sessionKey;

    /**
     * 学号(AES加密存储)
     */
    private String studentNo;

    /**
     * 学号后4位(用于展示)
     */
    private String studentNoSuffix;

    /**
     * 姓名(AES加密存储)
     */
    private String name;

    /**
     * 微信昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 班级编码
     */
    private String classId;

    /**
     * 班级名称
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

    /**
     * 入学年份
     */
    private Integer enrollYear;

    /**
     * 累计里程(公里)
     */
    private BigDecimal totalMileage;

    /**
     * 累计步数
     */
    private Long totalSteps;

    /**
     * 当前连续运动天数
     */
    private Integer continuousDays;

    /**
     * 历史最大连续天数
     */
    private Integer maxContinuousDays;

    /**
     * 最后同步日期
     */
    private LocalDate lastSyncDate;

    /**
     * 角色: 0-学生 1-管理员
     */
    private Integer role;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 判断是否已绑定学号
     */
    public boolean isBound() {
        return studentNo != null && !studentNo.isEmpty();
    }
}
