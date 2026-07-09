package com.ws.bitesmart.entity.user;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户地址簿 实体类
 *
 * 对应 user_address 表。
 * 一个用户可以有多个地址，通过 is_default 标记哪个是默认地址。
 */
@Data
public class UserAddress {

    private Long id;
    private Long userId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detailAddress;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 地址标签：家/公司/学校 */
    private String addressTag;

    /** 是否默认地址：1-是 0-否 */
    private Integer isDefault;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
