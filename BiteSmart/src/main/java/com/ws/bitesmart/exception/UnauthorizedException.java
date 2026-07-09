package com.ws.bitesmart.exception;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import lombok.Getter;

/**
 * 未授权异常（401）
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final int code;

    public UnauthorizedException() {
        super(ResultCodeEnum.UNAUTHORIZED.getMessage());
        this.code = ResultCodeEnum.UNAUTHORIZED.getCode();
    }

    public UnauthorizedException(String message) {
        super(message);
        this.code = ResultCodeEnum.UNAUTHORIZED.getCode();
    }

}
