package com.ws.bitesmart.exception;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 *
 * 用于在 Service 层主动抛出可预期的业务错误，
 * 由 GlobalExceptionHandler 统一捕获并返回给前端。
 *
 * 使用场景：
 *   // 方式一：使用预设的 ResultCodeEnum
 *   throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
 *
 *   // 方式二：自定义错误消息
 *   throw new BusinessException(ResultCodeEnum.STOCK_NOT_ENOUGH, "库存不足，当前仅剩" + stock + "份");
 *
 *   // 方式三：自定义错误码和消息
 *   throw new BusinessException(3001, "文件大小超出限制");
 *
 * 注意：
 *   - 与 UnauthorizedException 不同，BusinessException 的 HTTP 状态码为 200，
 *     通过 code 字段传递业务错误码，前端通过 code 判断业务是否成功。
 *   - code = 200 表示成功，code != 200 表示失败。
 *
 * @author BiteSmart
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，对应 ResultCodeEnum 或自定义编码 */
    private final int code;

    /**
     * 自定义错误消息，使用默认错误码（500）
     *
     * @param message 错误描述
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCodeEnum.INTERNAL_SERVER_ERROR.getCode();
    }

    /**
     * 使用预设的 ResultCodeEnum
     *
     * @param resultCode 预定义的错误码枚举
     */
    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用预设的 ResultCodeEnum，但覆盖错误消息
     *
     * @param resultCode 预定义的错误码枚举
     * @param message    自定义错误描述
     */
    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 完全自定义错误码和错误消息
     *
     * @param code    自定义错误码
     * @param message 自定义错误描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

}
