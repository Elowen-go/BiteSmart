package com.ws.bitesmart.exception;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import lombok.Getter;

/**
 * 未授权异常（401）
 *
 * 当用户未登录或 Token 失效时使用此异常。
 * 与 BusinessException 的区别：
 *   - BusinessException：HTTP 状态码为 200，通过 code 区分业务成功/失败
 *   - UnauthorizedException：HTTP 状态码为 401，前端收到 401 应跳转到登录页
 *
 * 使用场景：
 *   1. Controller 层需要明确返回 401 状态码
 *   2. 自定义权限校验失败
 *   3. Token 刷新时旧 Token 无效
 *
 * @author BiteSmart
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    /** 错误码，固定为 401 */
    private final int code;

    /**
     * 使用默认的未授权错误消息
     */
    public UnauthorizedException() {
        super(ResultCodeEnum.UNAUTHORIZED.getMessage());
        this.code = ResultCodeEnum.UNAUTHORIZED.getCode();
    }

    /**
     * 自定义未授权错误消息
     *
     * @param message 自定义错误描述
     */
    public UnauthorizedException(String message) {
        super(message);
        this.code = ResultCodeEnum.UNAUTHORIZED.getCode();
    }

}
