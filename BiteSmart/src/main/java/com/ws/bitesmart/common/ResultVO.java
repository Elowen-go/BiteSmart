package com.ws.bitesmart.common;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class ResultVO<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private ResultVO() {
        this.timestamp = System.currentTimeMillis();
    }

    private ResultVO(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功响应 ====================

    /** 无数据，只有默认消息 "操作成功" */
    public static <T> ResultVO<T> success() {
        return new ResultVO<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    /** 有数据，消息用默认的 "操作成功" */
    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    /** 自定义消息 + 数据 */
    public static <T> ResultVO<T> success(String message, T data) {
        return new ResultVO<>(ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    /** 只有自定义消息，没有数据。比如登出成功这种场景 */
    @SuppressWarnings("unchecked")
    public static <T> ResultVO<T> ok(String message) {
        return (ResultVO<T>) new ResultVO<>(ResultCodeEnum.SUCCESS.getCode(), message, null);
    }

    // ==================== 失败响应 ====================

    public static <T> ResultVO<T> error(ResultCodeEnum resultCode) {
        return new ResultVO<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> ResultVO<T> error(ResultCodeEnum resultCode, String message) {
        return new ResultVO<>(resultCode.getCode(), message, null);
    }

    public static <T> ResultVO<T> error(int code, String message) {
        return new ResultVO<>(code, message, null);
    }

    public static <T> ResultVO<T> error(String message) {
        return new ResultVO<>(ResultCodeEnum.INTERNAL_SERVER_ERROR.getCode(), message, null);
    }

}
