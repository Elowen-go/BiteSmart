package com.ws.bitesmart.entity.delivery;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送员信息表 实体类
 *
 * 对应 delivery_driver 表。
 * 用户注册为配送员时创建一条记录，关联 sys_user。
 */
@Data
public class DeliveryDriver {

    /** 主键ID */
    private Long id;

    /** 系统用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 联系电话 */
    private String phone;

    /** 身份证号 */
    private String idCard;

    /** 车辆类型：10-电动车 20-自行车 30-汽车 */
    private Integer vehicleType;

    /** 服务区域（JSON） */
    private String serviceArea;

    /** 当前纬度 */
    private BigDecimal currentLat;

    /** 当前经度 */
    private BigDecimal currentLng;

    /** 状态：10-在线 20-忙碌 30-离线 40-冻结 */
    private Integer status;

    /** 最大同时配送单数（默认5） */
    private Integer maxOrders;

    /** 当前配送单数 */
    private Integer currentOrders;

    /** 平均评分 */
    private BigDecimal avgRating;

    /** 累计配送次数 */
    private Integer totalDeliveries;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
