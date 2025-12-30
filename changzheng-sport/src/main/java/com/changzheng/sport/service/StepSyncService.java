package com.changzheng.sport.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.changzheng.common.entity.*;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import com.changzheng.common.util.RedisUtils;
import com.changzheng.sport.dto.ProgressVO;
import com.changzheng.sport.dto.SyncResult;
import com.changzheng.sport.mapper.*;
import com.changzheng.sport.mq.StepEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final RedisUtils redisUtils;
    private final StepEventProducer stepEventProducer;

    @Value("${sport.step-to-km-rate:2000}")
    private Integer stepToKmRate;

    @Value("${sport.daily-step-limit:30000}")
    private Integer dailyStepLimit;

    @Value("${sport.anomaly-threshold:50000}")
    private Integer anomalyThreshold;

    /**
     * 同步微信运动步数
     */
    @Transactional
    public SyncResult syncSteps(Long userId, JSONArray stepInfoList) {
        SyncResult result = new SyncResult();
        result.setSyncCount(0);
        result.setNewUnlockedNodes(new ArrayList<>());
        result.setNewAchievements(new ArrayList<>());

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        BigDecimal currentMileage = user.getTotalMileage();
        int syncCount = 0;

        // 获取用户注册日期，只同步注册之后的数据
        LocalDate registerDate = user.getCreatedAt() != null 
            ? user.getCreatedAt().toLocalDate() 
            : LocalDate.now();

        // 遍历微信返回的步数数据(最近30天)
        for (int i = 0; i < stepInfoList.size(); i++) {
            JSONObject stepInfo = stepInfoList.getJSONObject(i);
            long timestamp = stepInfo.getLong("timestamp");
            int steps = stepInfo.getInt("step");

            LocalDate recordDate = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toLocalDate();

            // 跳过注册日期之前的数据
            if (recordDate.isBefore(registerDate)) {
                log.debug("跳过注册前的步数数据: userId={}, date={}, registerDate={}", 
                        userId, recordDate, registerDate);
                continue;
            }

            // 幂等检查
            String idempotentKey = String.format("idempotent:step:%d:%s", userId, recordDate);
            if (!redisUtils.checkAndSetIdempotent(idempotentKey, 48, TimeUnit.HOURS)) {
                // 已处理过,检查是否需要更新(步数增加)
                DailySteps existing = dailyStepsMapper.selectByUserIdAndDate(userId, recordDate);
                if (existing != null && existing.getRawSteps() >= steps) {
                    continue; // 步数没有增加,跳过
                }
            }

            // 处理当日步数
            BigDecimal mileageDelta = processDailySteps(userId, recordDate, steps, currentMileage);
            currentMileage = currentMileage.add(mileageDelta);
            syncCount++;

            // 今日数据特殊处理
            if (recordDate.equals(LocalDate.now())) {
                result.setTodaySteps(steps);
                result.setTodayMileage(mileageDelta);
            }
        }

        // 更新用户累计里程和累计步数
        user.setTotalMileage(currentMileage);
        // 计算累计步数（从每日步数表汇总）
        Long totalValidSteps = dailyStepsMapper.sumValidStepsByUserId(userId);
        user.setTotalSteps(totalValidSteps != null ? totalValidSteps : 0L);
        user.setLastSyncDate(LocalDate.now());
        userMapper.updateById(user);

        // 检查节点解锁
        List<SyncResult.NodeInfo> newNodes = checkAndUnlockNodes(userId, currentMileage);
        result.setNewUnlockedNodes(newNodes);

        // 发送MQ事件
        if (syncCount > 0) {
            stepEventProducer.sendStepRecordedEvent(userId, LocalDate.now(), 
                    result.getTodaySteps() != null ? result.getTodaySteps() : 0, 
                    result.getTodayMileage() != null ? result.getTodayMileage() : BigDecimal.ZERO);
        }

        result.setSyncCount(syncCount);
        result.setTotalMileage(currentMileage);
        
        // 确保始终返回今日步数（即使没有新数据同步）
        if (result.getTodaySteps() == null) {
            DailySteps todayRecord = dailyStepsMapper.selectByUserIdAndDate(userId, LocalDate.now());
            if (todayRecord != null) {
                result.setTodaySteps(todayRecord.getValidSteps());
                result.setTodayMileage(BigDecimal.valueOf(todayRecord.getValidSteps())
                        .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN));
            } else {
                result.setTodaySteps(0);
                result.setTodayMileage(BigDecimal.ZERO);
            }
        }

        log.info("用户步数同步完成: userId={}, syncCount={}, totalMileage={}", 
                userId, syncCount, currentMileage);

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
            mileageDelta = BigDecimal.valueOf(stepsDiff)
                    .divide(BigDecimal.valueOf(stepToKmRate), 2, RoundingMode.DOWN);
        }

        // 5. 写入里程流水
        if (mileageDelta.compareTo(BigDecimal.ZERO) > 0) {
            MileageLedger ledger = new MileageLedger();
            ledger.setUserId(userId);
            ledger.setRecordDate(recordDate);
            ledger.setSteps(validSteps);
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

                // 发送节点解锁事件
                stepEventProducer.sendNodeUnlockedEvent(userId, node.getId(), LocalDateTime.now());

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

        ProgressVO vo = new ProgressVO();
        vo.setTotalMileage(user.getTotalMileage());
        vo.setTotalSteps(user.getTotalSteps());
        vo.setTotalDistance(BigDecimal.valueOf(25000)); // 长征总里程
        vo.setContinuousDays(user.getContinuousDays());

        // 计算进度百分比
        BigDecimal progress = user.getTotalMileage()
                .divide(BigDecimal.valueOf(25000), 4, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));
        vo.setProgressPercent(progress);

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
        vo.setUnlockedNodeCount(unlockedCount.intValue());
        vo.setTotalNodeCount(totalCount.intValue());

        // 当前节点和下一节点
        RouteNode currentNode = routeNodeMapper.selectCurrentNode(user.getTotalMileage());
        RouteNode nextNode = routeNodeMapper.selectNextNode(user.getTotalMileage());

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
            next.setRemainingMileage(nextNode.getMileageThreshold().subtract(user.getTotalMileage()));
            vo.setNextNode(next);
        }

        return vo;
    }
}
