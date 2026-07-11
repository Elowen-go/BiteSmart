package com.ws.bitesmart.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),
    NO_CONTENT(204, "删除成功"),

    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),

    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务异常码 1000-9999
    USERNAME_EXISTS(1001, "用户名已存在"),
    USERNAME_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "账号已被冻结"),
    TOKEN_EXPIRED(1005, "Token已过期"),
    TOKEN_INVALID(1006, "Token无效"),
    ROLE_NOT_MATCH(1007, "角色不匹配，请选择正确的角色类型"),

    STOCK_NOT_ENOUGH(2001, "库存不足"),
    ORDER_STATUS_ERROR(2002, "订单状态异常"),
    ORDER_NOT_FOUND(2003, "订单不存在"),
    PAYMENT_FAILED(2004, "支付失败"),
    REFUND_FAILED(2005, "退款失败"),

    FILE_UPLOAD_FAILED(3001, "文件上传失败"),
    FILE_SIZE_EXCEED(3002, "文件大小超出限制"),

    AI_SERVICE_ERROR(4001, "AI服务调用失败"),

    VALIDATION_FAILED(5001, "参数校验失败");

    private final int code;
    private final String message;

}
