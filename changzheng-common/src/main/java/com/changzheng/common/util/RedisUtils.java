package com.changzheng.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final StringRedisTemplate redisTemplate;

    // ==================== String 操作 ====================

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 设置值(仅当key不存在时) - 用于分布式锁
     */
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    // ==================== ZSet 操作(排行榜) ====================

    /**
     * 添加到排行榜
     */
    public Boolean zAdd(String key, String member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 增加分数
     */
    public Double zIncrBy(String key, String member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    /**
     * 获取排名(从大到小,0开始)
     */
    public Long zReverseRank(String key, String member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    /**
     * 获取分数
     */
    public Double zScore(String key, String member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * 获取排行榜(从大到小)
     */
    public Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 获取排行榜大小
     */
    public Long zCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    // ==================== Hash 操作 ====================

    public void hSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    // ==================== 幂等Key操作 ====================

    /**
     * 检查幂等key是否存在,不存在则设置
     */
    public boolean checkAndSetIdempotent(String key, long timeout, TimeUnit unit) {
        Boolean result = setIfAbsent(key, "1", timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取分布式锁
     */
    public boolean tryLock(String lockKey, String value, long timeout, TimeUnit unit) {
        Boolean result = setIfAbsent(lockKey, value, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String lockKey, String value) {
        String currentValue = get(lockKey);
        if (value.equals(currentValue)) {
            delete(lockKey);
        }
    }
}
