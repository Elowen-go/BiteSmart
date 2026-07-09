package com.ws.bitesmart.common.constant;

/**
 * 系统常量定义
 */
public interface Constant {

    // ==================== JWT ====================
    String TOKEN_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";
    String CLAIM_USER_ID = "userId";
    String CLAIM_ROLE_TYPE = "roleType";

    // ==================== Redis Key ====================
    String REDIS_TOKEN_BLACKLIST = "token:blacklist:";
    String REDIS_USER_INFO = "user:info:";
    String REDIS_DISH_STOCK = "dish:stock:";
    String REDIS_CAPTCHA = "captcha:";

    // ==================== 业务状态 ====================
    int STATUS_NORMAL = 10;
    int STATUS_DISABLED = 20;
    int STATUS_DELETED = 30;

    // ==================== 逻辑删除 ====================
    int DELETED_NO = 0;
    int DELETED_YES = 1;

}
