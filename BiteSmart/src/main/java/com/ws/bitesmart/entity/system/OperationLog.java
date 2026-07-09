package com.ws.bitesmart.entity.system;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志表 实体类
 *
 * 对应 operation_log 表。
 * 关键操作（登录、注册、下单、支付、取消订单、接单等）记录到此表。
 * 用于审计追溯和问题排查。
 */
@Data
public class OperationLog {

    private Long id;
    private Long userId;
    private String username;

    /** 用户角色 */
    private Integer roleType;

    /** 操作描述，如 "用户登录"、"创建订单" */
    private String operation;

    /** 请求方法（类名+方法名） */
    private String method;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 JSON */
    private String requestParams;

    /** 请求IP */
    private String requestIp;

    /** User-Agent */
    private String userAgent;

    /** 耗时（毫秒） */
    private Integer costTime;

    /** 状态：10-成功 20-失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    private LocalDateTime createTime;
    private Integer deleted;

}
