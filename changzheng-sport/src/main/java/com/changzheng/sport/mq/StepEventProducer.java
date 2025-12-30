package com.changzheng.sport.mq;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 步数事件生产者
 */
@Slf4j
@Component
public class StepEventProducer {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.enabled:false}")
    private boolean enabled;

    private static final String STEP_TOPIC = "STEP_TOPIC";
    private static final String NODE_TOPIC = "NODE_TOPIC";

    /**
     * 发送步数记录事件
     */
    public void sendStepRecordedEvent(Long userId, LocalDate date, int steps, BigDecimal mileage) {
        if (!enabled || rocketMQTemplate == null) {
            log.debug("RocketMQ未启用,跳过发送步数事件");
            return;
        }

        StepRecordedEvent event = new StepRecordedEvent();
        event.setUserId(userId);
        event.setDate(date.toString());
        event.setSteps(steps);
        event.setMileage(mileage);
        event.setTimestamp(System.currentTimeMillis());

        try {
            rocketMQTemplate.send(STEP_TOPIC + ":RECORDED", 
                    MessageBuilder.withPayload(JSONUtil.toJsonStr(event)).build());
            log.info("发送步数记录事件: userId={}, date={}", userId, date);
        } catch (Exception e) {
            log.error("发送步数记录事件失败", e);
        }
    }

    /**
     * 发送节点解锁事件
     */
    public void sendNodeUnlockedEvent(Long userId, Long nodeId, LocalDateTime unlockedAt) {
        if (!enabled || rocketMQTemplate == null) {
            log.debug("RocketMQ未启用,跳过发送节点解锁事件");
            return;
        }

        NodeUnlockedEvent event = new NodeUnlockedEvent();
        event.setUserId(userId);
        event.setNodeId(nodeId);
        event.setUnlockedAt(unlockedAt.toString());
        event.setTimestamp(System.currentTimeMillis());

        try {
            rocketMQTemplate.send(NODE_TOPIC + ":UNLOCKED", 
                    MessageBuilder.withPayload(JSONUtil.toJsonStr(event)).build());
            log.info("发送节点解锁事件: userId={}, nodeId={}", userId, nodeId);
        } catch (Exception e) {
            log.error("发送节点解锁事件失败", e);
        }
    }

    @Data
    public static class StepRecordedEvent {
        private Long userId;
        private String date;
        private Integer steps;
        private BigDecimal mileage;
        private Long timestamp;
    }

    @Data
    public static class NodeUnlockedEvent {
        private Long userId;
        private Long nodeId;
        private String unlockedAt;
        private Long timestamp;
    }
}
