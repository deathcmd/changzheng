package com.changzheng.sport.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.changzheng.common.entity.*;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import com.changzheng.sport.dto.ProgressVO;
import com.changzheng.sport.dto.SyncResult;
import com.changzheng.sport.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 步数同步服务 - 核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StepSyncService {

    private final DailyStepsMapper dailyStepsMapper;
    private final MileageLedgerMapper mileageLedgerMapper;
    private final UserNodeProgressMapper userNodeProgressMapper;
    private final RouteNodeMapper routeNodeMapper;
    private final UserMapper userMapper;

    @Value("${sport.step-to-km-rate:2000}")
    private Integer stepToKmRate;

    @Value("${sport.daily-step-limit:30000}")
    private Integer dailyStepLimit;

    @Value("${sport.anomaly-threshold:50000}")
    private Integer anomalyThreshold;

    @Value("${sport.total-distance:25000}")
    private BigDecimal totalDistance;

    @Value("${sport.max-sync-records:31}")
    private Integer maxSyncRecords;

    /**
     * 同步微信运动步数
     */
    @Transactional
    public SyncResult syncSteps(Long userId, JSONArray stepInfoList) {
        log.info("开始步数同步: userId={}, stepInfoListSize={}", userId, (stepInfoList != null ? stepInfoList.size() : "null"));
        SyncResult result = new SyncResult();
        result.setSyncCount(0);
        result.setNewUnlockedNodes(new ArrayList<>());
        result.setNewAchievements(new ArrayList<>());

        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        validateConfiguration();

        // Serialise synchronisations for the same user so that cumulative
        // mileage and the per-day ledger cannot diverge under concurrent calls.
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (stepInfoList == null || stepInfoList.isEmpty()) {
            return fillTodayStats(userId, result);
        }
        if (stepInfoList.size() > maxSyncRecords) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "步数记录数量超过限制");
        }

        BigDecimal currentMileage = user.getTotalMileage() == null ? BigDecimal.ZERO : user.getTotalMileage();
        int syncCount = 0;

        // 获取用户注册日期，只同步注册之后的数据
        LocalDate registerDate = user.getCreatedAt() != null 
            ? user.getCreatedAt().toLocalDate() 
            : LocalDate.now();

        // 按时间顺序处理，确保追加式流水的 before/after 具有可审计的时间顺序。
        for (StepRecord stepRecord : validateAndSortStepRecords(stepInfoList)) {
            LocalDate recordDate = stepRecord.recordDate();
            // 跳过注册日期之前的数据
            if (recordDate.isBefore(registerDate)) {
                log.debug("跳过注册前的步数数据: userId={}, date={}, registerDate={}", 
                        userId, recordDate, registerDate);
                continue;
            }

            // 处理当日步数
            BigDecimal mileageDelta = processDailySteps(userId, recordDate, stepRecord.steps(), currentMileage);
            if (mileageDelta == null) mileageDelta = BigDecimal.ZERO;
            
            currentMileage = currentMileage.add(mileageDelta);
            if (mileageDelta.signum() > 0) {
                syncCount++;
            }

        }

        // 更新用户累计里程和累计步数
        user.setTotalMileage(currentMileage);
        // 计算累计步数（从每日步数表汇总）
        Long totalValidSteps = dailyStepsMapper.sumValidStepsByUserId(userId);
        user.setTotalSteps(totalValidSteps != null ? totalValidSteps : 0L);
        user.setLastSyncDate(LocalDate.now());
        userMapper.updateById(user);
        log.info("用户数据更新完成: userId={}, totalSteps={}, totalMileage={}", userId, user.getTotalSteps(), currentMileage);

        // 检查节点解锁
        List<SyncResult.NodeInfo> newNodes = checkAndUnlockNodes(userId, currentMileage);
        result.setNewUnlockedNodes(newNodes);

        result.setSyncCount(syncCount);
        result.setTotalMileage(currentMileage);
        fillTodayStats(userId, result);

        return result;
    }

    private List<StepRecord> validateAndSortStepRecords(JSONArray stepInfoList) {
        List<StepRecord> records = new ArrayList<>(stepInfoList.size());
        LocalDate today = LocalDate.now();
        for (int i = 0; i < stepInfoList.size(); i++) {
            JSONObject stepInfo;
            Long timestamp;
            Integer steps;
            try {
                stepInfo = stepInfoList.getJSONObject(i);
                timestamp = stepInfo == null ? null : stepInfo.getLong("timestamp");
                steps = stepInfo == null ? null : stepInfo.getInt("step");
            } catch (RuntimeException exception) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "步数记录格式不正确");
            }
            if (stepInfo == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "步数记录格式不正确");
            }
            if (timestamp == null || timestamp <= 0 || steps == null || steps < 0) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "步数记录包含无效时间或步数");
            }
            try {
                LocalDate recordDate = Instant.ofEpochSecond(timestamp)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toLocalDate();
                if (recordDate.isAfter(today)) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "步数记录日期不能晚于今天");
                }
                records.add(new StepRecord(timestamp, steps, recordDate));
            } catch (DateTimeException exception) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "步数记录时间超出有效范围");
            }
        }
        records.sort(Comparator.comparingLong(StepRecord::timestamp));
        return records;
    }

    private record StepRecord(long timestamp, int steps, LocalDate recordDate) {
    }

    private void validateConfiguration() {
        if (stepToKmRate == null || stepToKmRate <= 0
                || dailyStepLimit == null || dailyStepLimit <= 0
                || anomalyThreshold == null || anomalyThreshold <= 0
                || maxSyncRecords == null || maxSyncRecords <= 0
                || totalDistance == null || totalDistance.signum() <= 0) {
            throw new IllegalStateException("Invalid sport service configuration");
        }
    }

    /**
     * 填充今日统计信息（兜底逻辑）
     */
    private SyncResult fillTodayStats(Long userId, SyncResult result) {
        DailySteps todayRecord = dailyStepsMapper.selectByUserIdAndDate(userId, LocalDate.now());
        if (todayRecord != null) {
            result.setTodaySteps(todayRecord.getValidSteps());
            result.setTodayMileage(BigDecimal.valueOf(todayRecord.getValidSteps())
                    .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN));
        } else {
            result.setTodaySteps(0);
            result.setTodayMileage(BigDecimal.ZERO);
        }

        User user = userMapper.selectById(userId);
        if (user != null) {
            result.setTotalMileage(user.getTotalMileage() == null ? BigDecimal.ZERO : user.getTotalMileage());
        }
        return result;
    }

    /**
     * 处理单日步数
     */
    private BigDecimal processDailySteps(Long userId, LocalDate recordDate, int rawSteps, 
                                          BigDecimal currentMileage) {
        // 1. 异常检测
        boolean isAnomaly = rawSteps > anomalyThreshold;
        String anomalyReason = isAnomaly ? "步数超过异常阈值" : null;

        // 2. 计算有效步数(上限裁剪)
        int validSteps = Math.min(rawSteps, dailyStepLimit);
        int creditedSteps = validSteps;

        // 3. 计算里程增量
        BigDecimal mileageDelta = BigDecimal.valueOf(validSteps)
                .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN);

        // 4. 保存每日步数(幂等)
        DailySteps dailySteps = dailyStepsMapper.selectByUserIdAndDate(userId, recordDate);
        if (dailySteps == null) {
            dailySteps = new DailySteps();
            dailySteps.setUserId(userId);
            dailySteps.setRecordDate(recordDate);
            dailySteps.setRawSteps(rawSteps);
            dailySteps.setValidSteps(validSteps);
            dailySteps.setSource("WECHAT");
            dailySteps.setIsAnomaly(isAnomaly ? 1 : 0);
            dailySteps.setAnomalyReason(anomalyReason);
            dailySteps.setSyncTime(LocalDateTime.now());
            dailyStepsMapper.insert(dailySteps);
        } else {
            if (dailySteps.getRawSteps() != null && rawSteps <= dailySteps.getRawSteps()) {
                return BigDecimal.ZERO;
            }
            // 更新(步数可能增加)
            int oldValidSteps = dailySteps.getValidSteps();
            dailySteps.setRawSteps(rawSteps);
            dailySteps.setValidSteps(validSteps);
            dailySteps.setIsAnomaly(isAnomaly ? 1 : 0);
            dailySteps.setAnomalyReason(anomalyReason);
            dailySteps.setSyncTime(LocalDateTime.now());
            dailyStepsMapper.updateById(dailySteps);

            // 里程增量只算差值
            int stepsDiff = validSteps - oldValidSteps;
            if (stepsDiff <= 0) {
                return BigDecimal.ZERO;
            }
            creditedSteps = stepsDiff;
            BigDecimal oldMileage = BigDecimal.valueOf(oldValidSteps)
                    .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN);
            BigDecimal newMileage = BigDecimal.valueOf(validSteps)
                    .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN);
            mileageDelta = newMileage.subtract(oldMileage);
        }

        // 5. 写入里程流水
        if (mileageDelta.compareTo(BigDecimal.ZERO) > 0) {
            MileageLedger ledger = new MileageLedger();
            ledger.setUserId(userId);
            ledger.setRecordDate(recordDate);
            ledger.setSteps(creditedSteps);
            ledger.setMileageDelta(mileageDelta);
            ledger.setMileageBefore(currentMileage);
            ledger.setMileageAfter(currentMileage.add(mileageDelta));
            ledger.setConversionRate(stepToKmRate);
            ledger.setDailyLimit(dailyStepLimit);
            ledger.setReason("DAILY_SYNC");
            ledger.setStatus(1);
            mileageLedgerMapper.insert(ledger);
        }

        return mileageDelta;
    }

    /**
     * 检查并解锁节点
     */
    private List<SyncResult.NodeInfo> checkAndUnlockNodes(Long userId, BigDecimal currentMileage) {
        List<SyncResult.NodeInfo> newUnlocked = new ArrayList<>();

        // 获取所有未解锁但已达到里程的节点
        List<RouteNode> allNodes = routeNodeMapper.selectList(
                new LambdaQueryWrapper<RouteNode>()
                        .eq(RouteNode::getStatus, 1)
                        .le(RouteNode::getMileageThreshold, currentMileage)
                        .orderByAsc(RouteNode::getSortOrder)
        );

        for (RouteNode node : allNodes) {
            // 检查是否已解锁
            UserNodeProgress progress = userNodeProgressMapper.selectByUserAndNode(userId, node.getId());
            
            if (progress == null) {
                // 首次解锁
                progress = new UserNodeProgress();
                progress.setUserId(userId);
                progress.setNodeId(node.getId());
                progress.setUnlockStatus(1);
                progress.setUnlockedAt(LocalDateTime.now());
                progress.setUnlockedMileage(currentMileage);
                progress.setViewStatus(0);
                progress.setViewCount(0);
                userNodeProgressMapper.insert(progress);

                SyncResult.NodeInfo nodeInfo = new SyncResult.NodeInfo();
                nodeInfo.setNodeId(node.getId());
                nodeInfo.setNodeName(node.getNodeName());
                nodeInfo.setMileageThreshold(node.getMileageThreshold());
                newUnlocked.add(nodeInfo);

                log.info("用户解锁节点: userId={}, nodeId={}, nodeName={}", 
                        userId, node.getId(), node.getNodeName());
            }
        }

        return newUnlocked;
    }

    /**
     * 获取用户进度
     */
    public ProgressVO getProgress(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "用户已被禁用");
        }

        ProgressVO vo = new ProgressVO();
        BigDecimal userMileage = user.getTotalMileage() == null ? BigDecimal.ZERO : user.getTotalMileage();
        vo.setTotalMileage(userMileage);
        vo.setTotalSteps(user.getTotalSteps() == null ? 0L : user.getTotalSteps());
        vo.setTotalDistance(totalDistance);
        vo.setContinuousDays(user.getContinuousDays() == null ? 0 : user.getContinuousDays());
        Integer totalDays = dailyStepsMapper.countDaysByUserId(userId);
        vo.setTotalDays(totalDays == null ? 0 : totalDays);

        // 计算进度百分比
        BigDecimal progress = userMileage
                .divide(totalDistance, 4, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));
        vo.setProgressPercent(progress.min(BigDecimal.valueOf(100)));

        // 获取今日步数
        DailySteps todaySteps = dailyStepsMapper.selectByUserIdAndDate(userId, LocalDate.now());
        if (todaySteps != null) {
            vo.setTodaySteps(todaySteps.getValidSteps());
            vo.setTodayMileage(BigDecimal.valueOf(todaySteps.getValidSteps())
                    .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN));
        } else {
            vo.setTodaySteps(0);
            vo.setTodayMileage(BigDecimal.ZERO);
        }

        // 获取已解锁节点数
        Long unlockedCount = userNodeProgressMapper.countUnlockedByUserId(userId);
        Long totalCount = routeNodeMapper.selectCount(
                new LambdaQueryWrapper<RouteNode>().eq(RouteNode::getStatus, 1));
        vo.setUnlockedNodeCount(unlockedCount == null ? 0 : unlockedCount.intValue());
        vo.setTotalNodeCount(totalCount == null ? 0 : totalCount.intValue());

        // 当前节点和下一节点
        RouteNode currentNode = routeNodeMapper.selectCurrentNode(userMileage);
        RouteNode nextNode = routeNodeMapper.selectNextNode(userMileage);

        if (currentNode != null) {
            ProgressVO.NodeInfo current = new ProgressVO.NodeInfo();
            current.setNodeId(currentNode.getId());
            current.setNodeName(currentNode.getNodeName());
            current.setMileageThreshold(currentNode.getMileageThreshold());
            vo.setCurrentNode(current);
        }

        if (nextNode != null) {
            ProgressVO.NodeInfo next = new ProgressVO.NodeInfo();
            next.setNodeId(nextNode.getId());
            next.setNodeName(nextNode.getNodeName());
            next.setMileageThreshold(nextNode.getMileageThreshold());
            next.setRemainingMileage(nextNode.getMileageThreshold().subtract(userMileage));
            vo.setNextNode(next);
        }

        return vo;
    }
}
