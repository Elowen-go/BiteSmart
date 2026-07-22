-- ============================================================
-- 20260719 骑手配送链路：拒单原因 / 订单备注快照 / 轨迹表
-- 对应 /api/driver/tasks/{id}/reject、/api/driver/location、
-- /api/delivery/tracking/{orderId} 扩展
-- ============================================================

USE bitesmart;

-- delivery_task：拒单原因 + 订单备注快照
-- （当前位置复用已有 current_lat / current_lng / location_update_time，无需加列）
ALTER TABLE `delivery_task`
    ADD COLUMN `reject_reason` varchar(255) DEFAULT NULL COMMENT '骑手拒单原因' AFTER `exception_reason`,
    ADD COLUMN `order_remark` varchar(500) DEFAULT NULL COMMENT '订单备注快照（创建任务时从订单拷贝）' AFTER `reject_reason`;

-- 骑手轨迹点表（坐标系约定：GCJ-02，高德/腾讯地图直接用）
CREATE TABLE IF NOT EXISTS `rider_location` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '配送任务ID',
  `driver_id` bigint(20) NOT NULL COMMENT '骑手ID（delivery_driver.id）',
  `latitude` decimal(10,7) NOT NULL COMMENT '纬度（GCJ-02）',
  `longitude` decimal(10,7) NOT NULL COMMENT '经度（GCJ-02）',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手配送轨迹点表';
