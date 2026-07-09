package com.ws.bitesmart.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色类型枚举
 */
@Getter
@AllArgsConstructor
public enum RoleTypeEnum {

    USER(10, "普通用户"),
    MERCHANT(20, "商家"),
    DELIVERY_DRIVER(30, "配送员"),
    ADMIN(40, "管理员");

    private final int code;
    private final String desc;

    public static RoleTypeEnum getByCode(int code) {
        for (RoleTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

}
