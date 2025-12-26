package com.changzheng.sport.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 同步结果
 */
@Data
public class SyncResult {
    private Integer syncCount;
    private Integer todaySteps;
    private BigDecimal todayMileage;
    private BigDecimal totalMileage;
    private List<NodeInfo> newUnlockedNodes;
    private List<AchievementInfo> newAchievements;

    @Data
    public static class NodeInfo {
        private Long nodeId;
        private String nodeName;
        private BigDecimal mileageThreshold;
    }

    @Data
    public static class AchievementInfo {
        private Long achievementId;
        private String achievementName;
    }
}
