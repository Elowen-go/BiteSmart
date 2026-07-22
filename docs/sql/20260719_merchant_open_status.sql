-- ============================================================
-- 20260719 店铺营业状态字段
-- merchant 增加 open_status：10-营业中 20-打烊
-- 对应后端 PUT /api/merchant/shop 的 openStatus 字段
-- ============================================================

USE bitesmart;

ALTER TABLE `merchant`
    ADD COLUMN `open_status` tinyint(4) NOT NULL DEFAULT 10 COMMENT '营业状态：10-营业中 20-打烊' AFTER `status`;
