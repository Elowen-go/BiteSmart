package com.ws.bitesmart.exception;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 通过 @RestControllerAdvice 拦截所有 Controller 抛出的异常，
 * 统一转换为标准响应格式 { code, message, data, timestamp } 返回给前端。
 *
 * 处理优先级：异常类型越精确优先级越高。
 * 例如 BusinessException 比 Exception 更具体，会优先匹配 BusinessException 的处理方法。
 *
 * @author BiteSmart
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 BusinessException
     *
     * 业务异常是主动抛出的可预期异常（如用户名已存在、库存不足等），
     * 使用 @ResponseStatus(HttpStatus.OK) 保持 HTTP 状态码为 200，
     * 通过业务 code 字段区分具体的错误类型，便于前端统一处理。
     *
     * @param e       业务异常对象，包含业务错误码和错误消息
     * @param request 当前请求对象，用于记录请求路径到日志
     * @return 统一响应体 ResultVO
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultVO<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: [{}] {} | 请求: {} {}", e.getCode(), e.getMessage(),
                request.getMethod(), request.getRequestURI());
        return ResultVO.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理未授权异常 UnauthorizedException
     *
     * 当用户未登录或 Token 失效时触发，HTTP 状态码设为 401，
     * 前端收到 401 后应跳转到登录页面。
     *
     * @param e       未授权异常对象
     * @param request 当前请求对象
     * @return 统一响应体 ResultVO
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResultVO<Void> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("未授权访问: {} | 请求: {} {}", e.getMessage(),
                request.getMethod(), request.getRequestURI());
        return ResultVO.error(ResultCodeEnum.UNAUTHORIZED, e.getMessage());
    }

    /**
     * 处理参数校验异常 MethodArgumentNotValidException
     *
     * 使用 @Valid 或 @Validated 注解自动校验请求参数时，
     * 如果校验失败会抛出此异常。将所有字段的错误信息拼接后返回。
     *
     * 示例请求体：
     *   {"name": "", "age": 150}
     * 返回消息：
     *   "名称不能为空, 年龄不能超过120"
     *
     * @param e 参数校验异常对象，包含所有字段的校验错误
     * @return 统一响应体 ResultVO
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 提取所有字段的错误消息，用逗号拼接
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ResultVO.error(ResultCodeEnum.VALIDATION_FAILED, message);
    }

    /**
     * 处理参数类型错误 IllegalArgumentException
     *
     * 当传入的参数不符合方法要求的类型或取值范围时触发。
     *
     * @param e 参数异常对象
     * @return 统一响应体 ResultVO
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ResultVO.error(ResultCodeEnum.BAD_REQUEST, e.getMessage());
    }

    /**
     * 兜底异常处理器（终极防线）
     *
     * 处理所有未被上述方法捕获的异常（如 NullPointerException、SQLException 等），
     * 避免直接将异常堆栈暴露给前端。记录完整错误堆栈以便排查，返回通用错误消息。
     *
     * @param e       异常对象
     * @param request 当前请求对象
     * @return 统一响应体 ResultVO
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultVO<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResultVO.error(ResultCodeEnum.INTERNAL_SERVER_ERROR, "服务器繁忙，请稍后重试");
    }

}
