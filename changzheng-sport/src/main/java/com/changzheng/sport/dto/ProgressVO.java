package com.changzheng.sport.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户进度VO
 */
@Data
public class ProgressVO {
    private BigDecimal totalMileage;
    private Long totalSteps;
    private BigDecimal totalDistance;  // 长征总里程
    private BigDecimal progressPercent;
    private Integer todaySteps;
    private BigDecimal todayMileage;
    private Integer continuousDays;
    private Integer totalDays;
    private NodeInfo currentNode;
    private NodeInfo nextNode;
    private Integer unlockedNodeCount;
    private Integer totalNodeCount;

    @Data
    public static class NodeInfo {
        private Long nodeId;
        private String nodeName;
        private BigDecimal mileageThreshold;
        private BigDecimal remainingMileage;
    }
}
