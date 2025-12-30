package com.changzheng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户节点进度实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_node_progress")
public class UserNodeProgress extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 解锁状态: 0-未解锁 1-已解锁
     */
    private Integer unlockStatus;

    /**
     * 首次解锁时间
     */
    private LocalDateTime unlockedAt;

    /**
     * 解锁时的累计里程
     */
    private BigDecimal unlockedMileage;

    /**
     * 观看状态: 0-未看 1-已看
     */
    private Integer viewStatus;

    /**
     * 首次观看时间
     */
    private LocalDateTime firstViewAt;

    /**
     * 观看次数
     */
    private Integer viewCount;

    /**
     * 最后观看时间
     */
    private LocalDateTime lastViewAt;
}
