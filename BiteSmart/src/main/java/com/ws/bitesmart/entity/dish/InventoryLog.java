package com.ws.bitesmart.entity.dish;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存变动流水表 实体类
 *
 * 对应 inventory_log 表。
 * 记录每次库存变动的明细，包括入库、出库、下单锁定、支付扣减、取消释放等。
 */
@Data
public class InventoryLog {

    /** 主键ID */
    private Long id;

    /** 菜品ID */
    private Long dishId;

    /** 商家ID */
    private Long merchantId;

    /** 变动类型：10-入库 20-出库 30-下单锁定 40-支付扣减 50-取消释放 60-退款恢复 70-盘盈 80-盘亏 */
    private Integer changeType;

    /** 变动数量（正数增加，负数减少） */
    private Integer changeQuantity;

    /** 变动前库存 */
    private Integer beforeStock;

    /** 变动后库存 */
    private Integer afterStock;

    /** 变动前锁定库存 */
    private Integer beforeLockStock;

    /** 变动后锁定库存 */
    private Integer afterLockStock;

    /** 业务类型（order/cancel/refund等） */
    private String bizType;

    /** 关联业务ID（订单ID等） */
    private Long bizId;

    /** 操作人ID */
    private Long operatorId;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;
    private Integer deleted;

}
