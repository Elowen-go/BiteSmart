-- 订单配送方式与配送费，供骑手收入闭环使用
USE bitesmart;

ALTER TABLE `orders`
    ADD COLUMN `delivery_type` tinyint NOT NULL DEFAULT 10 COMMENT '配送方式：10-外卖配送 20-到店自取' AFTER `pay_amount`,
    ADD COLUMN `delivery_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单配送费，外卖配送默认5元' AFTER `delivery_type`;
