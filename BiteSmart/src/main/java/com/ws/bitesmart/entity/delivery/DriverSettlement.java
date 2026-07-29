package com.ws.bitesmart.entity.delivery;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送员结算记录表 实体类
 *
 * 对应 driver_settlement 表。
 * 配送任务完成后生成结算记录，记录配送费、奖励、罚款及结算金额。
 */
@Data
public class DriverSettlement {

    /** 主键ID */
    private Long id;

    /** 配送员ID */
    private Long driverId;

    /** 配送任务ID */
    private Long deliveryTaskId;

    /** 订单ID */
    private Long orderId;

    /** 管理员结算列表联表带出的骑手姓名 */
    private String driverName;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 奖励金额 */
    private BigDecimal bonus;

    /** 罚款金额 */
    private BigDecimal penalty;

    /** 结算金额（配送费 + 奖励 - 罚款） */
    private BigDecimal settlementAmount;

    /** 结算状态：10-待结算 20-已结算 30-异常 */
    private Integer settlementStatus;

    /** 结算时间 */
    private LocalDateTime settlementTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
