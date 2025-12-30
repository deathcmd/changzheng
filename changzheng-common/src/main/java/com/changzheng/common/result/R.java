package com.changzheng.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class R<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> result = new R<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> result = new R<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> R<T> fail() {
        return fail(ResultCode.FAILED);
    }

    public static <T> R<T> fail(String message) {
        R<T> result = new R<>();
        result.setCode(ResultCode.FAILED.getCode());
        result.setMessage(message);
        return result;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> result = new R<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> R<T> fail(ResultCode resultCode, T data) {
        R<T> result = new R<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        result.setData(data);
        return result;
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
