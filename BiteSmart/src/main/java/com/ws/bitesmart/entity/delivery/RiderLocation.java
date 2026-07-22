package com.ws.bitesmart.entity.delivery;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑手配送轨迹点表 实体类
 *
 * 对应 rider_location 表。
 * 骑手在配送中定时上报坐标，用户端按 task_id 查询轨迹回放。
 * 坐标系约定：GCJ-02（高德/腾讯地图直接使用）。
 */
@Data
public class RiderLocation {

    private Long id;

    /** 配送任务ID */
    private Long taskId;

    /** 骑手ID（delivery_driver.id） */
    private Long driverId;

    /** 纬度（GCJ-02） */
    private BigDecimal latitude;

    /** 经度（GCJ-02） */
    private BigDecimal longitude;

    /** 上报时间 */
    private LocalDateTime createdTime;

}
