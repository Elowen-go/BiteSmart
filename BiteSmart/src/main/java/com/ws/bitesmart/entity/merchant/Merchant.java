package com.ws.bitesmart.entity.merchant;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家信息表 实体类
 *
 * 对应 merchant 表。
 * 用户注册为商家时创建一条记录，status=10 待审核，
 * 管理员审核通过后 status=20 才能正常营业。
 * merchant.id 和 sys_user.id 是同一个值。
 */
@Data
public class Merchant {

    /** 主键ID，与 sys_user.id 一致 */
    private Long id;

    /** 系统用户ID */
    private Long userId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺Logo */
    private String shopLogo;

    /** 营业执照图片URL */
    private String businessLicense;

    /** 营业执照编号 */
    private String licenseNumber;

    /** 联系人姓名 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 店铺地址 */
    private String shopAddress;

    /** 配送范围 JSON */
    private String deliveryRange;

    /** 营业时间 JSON */
    private String businessHours;

    /** 店铺公告 */
    private String shopNotice;

    /** 状态：10-待审核 20-审核通过 30-审核驳回 40-已关闭 */
    private Integer status;

    /** 营业状态：10-营业中 20-打烊 */
    public static final int OPEN_STATUS_OPEN = 10;
    public static final int OPEN_STATUS_CLOSED = 20;

    /** 营业状态：10-营业中 20-打烊 */
    private Integer openStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 平均评分 */
    private BigDecimal avgRating;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
