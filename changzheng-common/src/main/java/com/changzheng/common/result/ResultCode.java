package com.changzheng.common.result;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ResultCode {

    // 通用成功/失败
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),

    // 参数校验 400xx
    PARAM_ERROR(40000, "参数错误"),
    PARAM_MISSING(40001, "缺少必要参数"),
    PARAM_INVALID(40002, "参数格式不正确"),

    // 认证相关 401xx
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    TOKEN_EXPIRED(40101, "Token已过期"),
    TOKEN_INVALID(40102, "Token无效"),
    REFRESH_TOKEN_EXPIRED(40103, "刷新Token已过期"),

    // 微信相关 402xx
    WX_CODE_INVALID(40201, "微信code无效"),
    WX_API_ERROR(40202, "微信接口调用失败"),
    WX_DECRYPT_ERROR(40203, "微信数据解密失败"),
    WX_SESSION_EXPIRED(40204, "微信session已过期，请重新登录"),

    // 权限相关 403xx
    FORBIDDEN(40300, "没有访问权限"),
    NODE_NOT_UNLOCKED(40301, "节点尚未解锁"),

    // 资源相关 404xx
    NOT_FOUND(40400, "资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    NODE_NOT_FOUND(40402, "节点不存在"),

    // 业务相关 409xx
    STUDENT_ALREADY_BOUND(40901, "该学号已被绑定"),
    USER_ALREADY_BOUND(40902, "您已绑定学号"),
    SYNC_TOO_FREQUENT(40903, "同步过于频繁，请稍后再试"),
    DATA_DUPLICATE(40904, "数据重复"),

    // 系统错误 500xx
    SYSTEM_ERROR(50000, "系统内部错误"),
    SERVICE_UNAVAILABLE(50001, "服务暂不可用"),
    DATABASE_ERROR(50002, "数据库操作失败"),
    REDIS_ERROR(50003, "缓存操作失败"),
    MQ_ERROR(50004, "消息队列操作失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
