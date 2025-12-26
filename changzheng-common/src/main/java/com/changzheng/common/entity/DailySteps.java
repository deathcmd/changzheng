package com.changzheng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日步数实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_daily_steps")
public class DailySteps extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 原始步数(微信返回)
     */
    private Integer rawSteps;

    /**
     * 有效步数(上限裁剪后)
     */
    private Integer validSteps;

    /**
     * 数据来源: WECHAT/MANUAL/SYSTEM
     */
    private String source;

    /**
     * 是否异常: 0-正常 1-异常
     */
    private Integer isAnomaly;

    /**
     * 异常原因
     */
    private String anomalyReason;

    /**
     * 微信加密数据(备查)
     */
    private String encryptedData;

    /**
     * 微信加密iv
     */
    private String iv;

    /**
     * 数据签名(防篡改)
     */
    private String signature;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;
}
