package com.ws.bitesmart.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户表 实体类
 *
 * 对应数据库表 sys_user，就是项目的用户体系核心。
 * 四种角色共用这一张表，通过 role_type 区分（10/20/30/40）。
 * 密码存的是 BCrypt 加密后的密文，不是明文。
 */
@Data
public class SysUser {

    /** 主键ID，雪花算法生成 */
    private Long id;

    /** 登录账号，一般是手机号或用户名，唯一 */
    private String username;

    /** 登录密码，BCrypt加密后的密文。绝不序列化到任何接口响应（登录/注册走专用DTO，不受影响） */
    @JsonIgnore
    private String password;

    /** 用户昵称，可以没有 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /**
     * 角色类型：10-普通用户 20-商家 30-配送员 40-管理员
     * @see com.ws.bitesmart.common.enums.RoleTypeEnum
     */
    private Integer roleType;

    /**
     * 账号状态：10-正常 20-冻结 30-注销
     */
    private Integer status;

    /**
     * 注册来源：10-PC 20-小程序 30-后台添加
     */
    private Integer registerSource;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删 1-已删
     * 所有查询都要加上 deleted = 0 条件，避免查到已删除的数据
     */
    private Integer deleted;

}
