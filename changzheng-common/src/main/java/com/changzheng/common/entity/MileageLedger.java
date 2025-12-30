package com.changzheng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 里程流水实体(核心对账表)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_mileage_ledger")
public class MileageLedger extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 当日有效步数
     */
    private Integer steps;

    /**
     * 里程增量(公里)
     */
    private BigDecimal mileageDelta;

    /**
     * 变更前累计里程
     */
    private BigDecimal mileageBefore;

    /**
     * 变更后累计里程
     */
    private BigDecimal mileageAfter;

    /**
     * 换算比例(步/公里)
     */
    private Integer conversionRate;

    /**
     * 当日上限步数
     */
    private Integer dailyLimit;

    /**
     * 变更原因: DAILY_SYNC/MANUAL_FIX/RECALC
     */
    private String reason;

    /**
     * 状态: 0-作废 1-有效
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作人ID(补算时)
     */
    private Long operatorId;
}
