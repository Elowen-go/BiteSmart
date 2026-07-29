USE bitesmart;

CREATE TABLE IF NOT EXISTS `membership_payment_order` (
  `id` bigint NOT NULL COMMENT '会员支付订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '支付宝商户订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_id` bigint NOT NULL COMMENT '会员套餐ID',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '应付金额',
  `status` tinyint NOT NULL DEFAULT '10' COMMENT '10-待支付 20-已支付 30-已关闭',
  `transaction_no` varchar(128) DEFAULT NULL COMMENT '支付宝交易号',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_payment_order_no` (`order_no`),
  KEY `idx_membership_payment_user` (`user_id`),
  KEY `idx_membership_payment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员支付宝支付订单';
