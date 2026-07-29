-- ============================================================
-- 20260728 配送导航坐标：商家取货点、订单收货点、配送任务坐标快照
-- 坐标系：GCJ-02
-- ============================================================

USE bitesmart;

ALTER TABLE `merchant`
    ADD COLUMN `shop_lat` decimal(10,7) DEFAULT NULL COMMENT '店铺纬度（GCJ-02）' AFTER `shop_address`,
    ADD COLUMN `shop_lng` decimal(10,7) DEFAULT NULL COMMENT '店铺经度（GCJ-02）' AFTER `shop_lat`;

ALTER TABLE `orders`
    ADD COLUMN `delivery_lat` decimal(10,7) DEFAULT NULL COMMENT '收货地址纬度（GCJ-02）' AFTER `delivery_address`,
    ADD COLUMN `delivery_lng` decimal(10,7) DEFAULT NULL COMMENT '收货地址经度（GCJ-02）' AFTER `delivery_lat`;

ALTER TABLE `delivery_task`
    ADD COLUMN `merchant_lat` decimal(10,7) DEFAULT NULL COMMENT '商家取货点纬度（GCJ-02）' AFTER `delivery_address`,
    ADD COLUMN `merchant_lng` decimal(10,7) DEFAULT NULL COMMENT '商家取货点经度（GCJ-02）' AFTER `merchant_lat`,
    ADD COLUMN `delivery_lat` decimal(10,7) DEFAULT NULL COMMENT '收货点纬度（GCJ-02）' AFTER `merchant_lng`,
    ADD COLUMN `delivery_lng` decimal(10,7) DEFAULT NULL COMMENT '收货点经度（GCJ-02）' AFTER `delivery_lat`;
