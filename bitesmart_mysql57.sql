SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `session_id` varchar(64) NOT NULL,
  `question` text NOT NULL,
  `answer` text NOT NULL,
  `model_name` varchar(64) DEFAULT 'qwen-plus',
  `prompt_tokens` int DEFAULT '0',
  `completion_tokens` int DEFAULT '0',
  `total_tokens` int DEFAULT '0',
  `cost` decimal(10,6) DEFAULT '0.000000',
  `response_time` int DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `ai_recommend_rule`;
CREATE TABLE `ai_recommend_rule` (
  `id` bigint NOT NULL,
  `rule_name` varchar(128) NOT NULL,
  `target_goal` varchar(64) NOT NULL,
  `prompt_template` text NOT NULL,
  `calorie_float` int DEFAULT '10',
  `max_calories` int DEFAULT NULL,
  `min_calories` int DEFAULT NULL,
  `protein_ratio` decimal(5,2) DEFAULT NULL,
  `fat_ratio` decimal(5,2) DEFAULT NULL,
  `carbs_ratio` decimal(5,2) DEFAULT NULL,
  `model_version` varchar(32) DEFAULT 'v1.0',
  `status` tinyint DEFAULT '10',
  `priority` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_goal_status` (`target_goal`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `combo`;
CREATE TABLE `combo` (
  `id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `combo_name` varchar(128) NOT NULL,
  `combo_image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `original_price` decimal(10,2) DEFAULT NULL,
  `combo_type` tinyint DEFAULT '10',
  `suitable_for` json DEFAULT NULL,
  `total_calories` int DEFAULT '0',
  `total_protein` decimal(8,2) DEFAULT '0.00',
  `total_fat` decimal(8,2) DEFAULT '0.00',
  `total_carbs` decimal(8,2) DEFAULT '0.00',
  `description` varchar(255) DEFAULT NULL,
  `replaceable_dish_pool` json DEFAULT NULL,
  `max_replace_count` int DEFAULT '0',
  `status` tinyint DEFAULT '10',
  `sales_count` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_type_status` (`combo_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `combo_dish_rel`;
CREATE TABLE `combo_dish_rel` (
  `id` bigint NOT NULL,
  `combo_id` bigint NOT NULL,
  `dish_id` bigint NOT NULL,
  `quantity` int DEFAULT '1',
  `is_fixed` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_combo_dish` (`combo_id`,`dish_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `complaint_ticket`;
CREATE TABLE `complaint_ticket` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `target_type` tinyint NOT NULL,
  `target_id` bigint NOT NULL,
  `complaint_reason` varchar(255) NOT NULL,
  `complaint_desc` varchar(500) DEFAULT NULL,
  `evidence_images` json DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `admin_operator_id` bigint DEFAULT NULL,
  `admin_remark` varchar(255) DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `coupon_template`;
CREATE TABLE `coupon_template` (
  `id` bigint NOT NULL,
  `merchant_id` bigint DEFAULT '0',
  `coupon_name` varchar(128) NOT NULL,
  `type` tinyint NOT NULL,
  `value` decimal(10,2) NOT NULL,
  `threshold` decimal(10,2) DEFAULT '0.00',
  `total_quantity` int DEFAULT '0',
  `used_quantity` int DEFAULT '0',
  `user_limit` int DEFAULT '1',
  `start_time` datetime NOT NULL,
  `expire_time` datetime NOT NULL,
  `status` tinyint DEFAULT '10',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_time_status` (`start_time`,`expire_time`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `data_backup_record`;
CREATE TABLE `data_backup_record` (
  `id` bigint NOT NULL,
  `backup_name` varchar(128) NOT NULL,
  `backup_type` tinyint DEFAULT '10',
  `file_path` varchar(255) NOT NULL,
  `file_size` bigint DEFAULT '0',
  `status` tinyint DEFAULT '10',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `error_msg` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `delivery_driver`;
CREATE TABLE `delivery_driver` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `real_name` varchar(64) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `id_card` varchar(18) DEFAULT NULL,
  `vehicle_type` tinyint DEFAULT '10',
  `service_area` json DEFAULT NULL,
  `current_lat` decimal(10,7) DEFAULT NULL,
  `current_lng` decimal(10,7) DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `max_orders` int DEFAULT '5',
  `current_orders` int DEFAULT '0',
  `avg_rating` decimal(2,1) DEFAULT '0.0',
  `total_deliveries` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_current_location` (`current_lat`,`current_lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `delivery_task`;
CREATE TABLE `delivery_task` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `order_no` varchar(32) NOT NULL,
  `driver_id` bigint DEFAULT NULL,
  `merchant_id` bigint NOT NULL,
  `merchant_address` varchar(255) NOT NULL,
  `merchant_phone` varchar(20) NOT NULL,
  `delivery_address` varchar(255) NOT NULL,
  `merchant_lat` decimal(10,7) DEFAULT NULL,
  `merchant_lng` decimal(10,7) DEFAULT NULL,
  `delivery_lat` decimal(10,7) DEFAULT NULL,
  `delivery_lng` decimal(10,7) DEFAULT NULL,
  `receiver_name` varchar(64) NOT NULL,
  `receiver_phone` varchar(20) NOT NULL,
  `pickup_code` varchar(16) DEFAULT NULL,
  `task_status` tinyint DEFAULT '10',
  `current_lat` decimal(10,7) DEFAULT NULL,
  `current_lng` decimal(10,7) DEFAULT NULL,
  `location_update_time` datetime DEFAULT NULL,
  `pickup_time` datetime DEFAULT NULL,
  `deliver_time` datetime DEFAULT NULL,
  `estimated_delivery_time` datetime DEFAULT NULL,
  `route_json` json DEFAULT NULL,
  `exception_reason` varchar(255) DEFAULT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `order_remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_current_location` (`current_lat`,`current_lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `diet_record`;
CREATE TABLE `diet_record` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `record_date` date NOT NULL,
  `record_time` time DEFAULT NULL,
  `meal_type` tinyint DEFAULT '10',
  `food_name` varchar(128) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `calories` int DEFAULT '0',
  `protein` decimal(8,2) DEFAULT '0.00',
  `fat` decimal(8,2) DEFAULT '0.00',
  `carbs` decimal(8,2) DEFAULT '0.00',
  `source_type` tinyint DEFAULT '20',
  `order_item_id` bigint DEFAULT NULL,
  `plan_meal_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_diet_record_order_item` (`order_item_id`),
  KEY `idx_user_date` (`user_id`,`record_date`),
  KEY `idx_plan_meal_id` (`plan_meal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
  `id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `dish_name` varchar(128) NOT NULL,
  `dish_image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `original_price` decimal(10,2) DEFAULT NULL,
  `stock` int NOT NULL DEFAULT '0',
  `lock_stock` int DEFAULT '0',
  `min_stock_warning` int DEFAULT '10',
  `sales_count` int DEFAULT '0',
  `sales_real` int DEFAULT '0',
  `unit` varchar(16) DEFAULT '份',
  `description` varchar(255) DEFAULT NULL,
  `suitable_for` json DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `ai_comment` varchar(500) DEFAULT NULL,
  `fit_scenes` json DEFAULT NULL,
  `cautions` json DEFAULT NULL,
  `calories` int DEFAULT '0',
  `protein` decimal(8,2) DEFAULT '0.00',
  `fat` decimal(8,2) DEFAULT '0.00',
  `carbs` decimal(8,2) DEFAULT '0.00',
  `status` tinyint DEFAULT '10',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status_stock` (`status`,`stock`),
  KEY `idx_sales_real` (`sales_real`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `dish_category`;
CREATE TABLE `dish_category` (
  `id` bigint NOT NULL,
  `category_name` varchar(64) NOT NULL,
  `category_icon` varchar(255) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `parent_id` bigint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `dish_ingredient`;
CREATE TABLE `dish_ingredient` (
  `id` bigint NOT NULL,
  `dish_id` bigint NOT NULL,
  `ingredient_id` bigint NOT NULL,
  `weight` decimal(10,2) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `dish_nutrition`;
CREATE TABLE `dish_nutrition` (
  `id` bigint NOT NULL,
  `dish_id` bigint NOT NULL,
  `calories` int DEFAULT '0',
  `protein` decimal(8,2) DEFAULT '0.00',
  `fat` decimal(8,2) DEFAULT '0.00',
  `carbs` decimal(8,2) DEFAULT '0.00',
  `fiber` decimal(8,2) DEFAULT '0.00',
  `sodium` decimal(8,2) DEFAULT '0.00',
  `cholesterol` decimal(8,2) DEFAULT '0.00',
  `vitamin_a` decimal(8,2) DEFAULT '0.00',
  `calcium` decimal(8,2) DEFAULT '0.00',
  `iron` decimal(8,2) DEFAULT '0.00',
  `nutrition_json` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `driver_settlement`;
CREATE TABLE `driver_settlement` (
  `id` bigint NOT NULL,
  `driver_id` bigint NOT NULL,
  `delivery_task_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `delivery_fee` decimal(10,2) NOT NULL,
  `bonus` decimal(10,2) DEFAULT '0.00',
  `penalty` decimal(10,2) DEFAULT '0.00',
  `settlement_amount` decimal(10,2) NOT NULL,
  `settlement_status` tinyint DEFAULT '10',
  `settlement_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_task_id` (`delivery_task_id`),
  KEY `idx_settlement_status` (`settlement_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `exercise_library`;
CREATE TABLE `exercise_library` (
  `id` bigint NOT NULL,
  `category` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `std_text` varchar(64) DEFAULT NULL,
  `kcal_per_min` decimal(6,2) DEFAULT NULL,
  `def_mins` int DEFAULT '30',
  `def_dist` decimal(6,2) DEFAULT '0.00',
  `image_url` varchar(255) DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `hot` tinyint(1) NOT NULL DEFAULT '0',
  `sort` int NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category_sort` (`category`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `exercise_record`;
CREATE TABLE `exercise_record` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `record_date` date NOT NULL,
  `exercise_type` varchar(64) NOT NULL,
  `duration` int DEFAULT '0',
  `distance` decimal(8,2) DEFAULT '0.00',
  `calories_burned` int DEFAULT '0',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `file_upload_record`;
CREATE TABLE `file_upload_record` (
  `id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_url` varchar(512) NOT NULL,
  `file_size` bigint DEFAULT '0',
  `mime_type` varchar(64) DEFAULT NULL,
  `storage_path` varchar(255) DEFAULT NULL,
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `ingredient`;
CREATE TABLE `ingredient` (
  `id` bigint NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `category_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `calories` decimal(10,2) DEFAULT NULL,
  `protein` decimal(10,2) DEFAULT NULL,
  `fat` decimal(10,2) DEFAULT NULL,
  `carbs` decimal(10,2) DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `inventory_log`;
CREATE TABLE `inventory_log` (
  `id` bigint NOT NULL,
  `dish_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `change_type` tinyint NOT NULL,
  `change_quantity` int NOT NULL,
  `before_stock` int NOT NULL,
  `after_stock` int NOT NULL,
  `before_lock_stock` int DEFAULT '0',
  `after_lock_stock` int DEFAULT '0',
  `biz_type` varchar(64) DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `membership_plan`;
CREATE TABLE `membership_plan` (
  `id` bigint NOT NULL,
  `plan_name` varchar(64) NOT NULL,
  `plan_type` tinyint NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `original_price` decimal(10,2) DEFAULT NULL,
  `valid_days` int NOT NULL,
  `benefits` json DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `sort_order` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`plan_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `shop_name` varchar(128) NOT NULL,
  `shop_logo` varchar(255) DEFAULT NULL,
  `business_license` varchar(255) DEFAULT NULL,
  `license_number` varchar(64) DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(20) DEFAULT NULL,
  `shop_address` varchar(255) DEFAULT NULL,
  `shop_lat` decimal(10,7) DEFAULT NULL,
  `shop_lng` decimal(10,7) DEFAULT NULL,
  `delivery_range` json DEFAULT NULL,
  `business_hours` json DEFAULT NULL,
  `shop_notice` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `open_status` tinyint NOT NULL DEFAULT '10',
  `audit_remark` varchar(255) DEFAULT NULL,
  `avg_rating` decimal(2,1) DEFAULT '0.0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_shop_name` (`shop_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `merchant_audit_log`;
CREATE TABLE `merchant_audit_log` (
  `id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `submit_time` datetime NOT NULL,
  `submit_data` json DEFAULT NULL,
  `audit_operator_id` bigint DEFAULT NULL,
  `audit_operator_name` varchar(64) DEFAULT NULL,
  `audit_status` tinyint NOT NULL,
  `audit_remark` varchar(255) DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_audit_time` (`audit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `merchant_fund_account`;
CREATE TABLE `merchant_fund_account` (
  `id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `pending_balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `available_balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `frozen_balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_income` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_refund` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_commission` decimal(12,2) NOT NULL DEFAULT '0.00',
  `version` bigint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`),
  KEY `idx_account_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `merchant_fund_ledger`;
CREATE TABLE `merchant_fund_ledger` (
  `id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `payment_log_id` bigint DEFAULT NULL,
  `refund_id` bigint DEFAULT NULL,
  `settlement_id` bigint DEFAULT NULL,
  `ledger_type` tinyint NOT NULL,
  `direction` tinyint NOT NULL,
  `balance_scope` tinyint NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `balance_before` decimal(12,2) NOT NULL,
  `balance_after` decimal(12,2) NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '20',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ledger_idempotency` (`idempotency_key`),
  KEY `idx_ledger_merchant_time` (`merchant_id`,`create_time`),
  KEY `idx_ledger_order_id` (`order_id`),
  KEY `idx_ledger_refund_id` (`refund_id`),
  KEY `idx_ledger_settlement_id` (`settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `merchant_settlement`;
CREATE TABLE `merchant_settlement` (
  `id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `settlement_no` varchar(32) NOT NULL,
  `period_start` datetime DEFAULT NULL,
  `period_end` datetime DEFAULT NULL,
  `gross_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `commission_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `refund_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `net_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `settlement_status` tinyint NOT NULL DEFAULT '10',
  `payout_method` varchar(32) DEFAULT NULL,
  `payout_account_mask` varchar(128) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_remark` varchar(255) DEFAULT NULL,
  `paid_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_settlement_merchant_time` (`merchant_id`,`create_time`),
  KEY `idx_settlement_status` (`settlement_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `message_notification`;
CREATE TABLE `message_notification` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `msg_type` tinyint DEFAULT '10',
  `title` varchar(128) NOT NULL,
  `content` varchar(500) NOT NULL,
  `jump_url` varchar(255) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `read_time` datetime DEFAULT NULL,
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `nutrition_standard`;
CREATE TABLE `nutrition_standard` (
  `id` bigint NOT NULL,
  `standard_name` varchar(128) NOT NULL,
  `gender` tinyint DEFAULT NULL,
  `age_min` int DEFAULT NULL,
  `age_max` int DEFAULT NULL,
  `activity_level` tinyint DEFAULT NULL,
  `target_goal` varchar(64) DEFAULT NULL,
  `daily_calories` int NOT NULL,
  `protein_grams` decimal(8,2) NOT NULL,
  `fat_grams` decimal(8,2) NOT NULL,
  `carbs_grams` decimal(8,2) NOT NULL,
  `water_ml` int DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_gender_age_activity` (`gender`,`age_min`,`age_max`,`activity_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `role_type` tinyint DEFAULT NULL,
  `operation` varchar(255) NOT NULL,
  `method` varchar(128) DEFAULT NULL,
  `request_url` varchar(255) DEFAULT NULL,
  `request_params` json DEFAULT NULL,
  `request_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `cost_time` int DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `error_msg` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `operation_log_detail`;
CREATE TABLE `operation_log_detail` (
  `id` bigint NOT NULL,
  `log_id` bigint NOT NULL,
  `field_name` varchar(64) NOT NULL,
  `old_value` text,
  `new_value` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_id` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `item_type` tinyint NOT NULL,
  `dish_id` bigint DEFAULT NULL,
  `combo_id` bigint DEFAULT NULL,
  `snapshot_name` varchar(128) NOT NULL,
  `snapshot_image` varchar(255) DEFAULT NULL,
  `snapshot_price` decimal(10,2) NOT NULL,
  `snapshot_calories` int DEFAULT '0',
  `snapshot_protein` decimal(8,2) DEFAULT '0.00',
  `snapshot_fat` decimal(8,2) DEFAULT '0.00',
  `snapshot_carbs` decimal(8,2) DEFAULT '0.00',
  `snapshot_nutrition_json` json DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `sub_total` decimal(10,2) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_combo_id` (`combo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `order_status_log`;
CREATE TABLE `order_status_log` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_status` tinyint DEFAULT NULL,
  `to_status` tinyint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'database_trigger',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_status_log_order` (`order_id`,`create_time`),
  KEY `idx_order_status_log_status` (`to_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `delivery_driver_id` bigint DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `discount_amount` decimal(10,2) DEFAULT '0.00',
  `pay_amount` decimal(10,2) NOT NULL,
  `delivery_type` tinyint NOT NULL DEFAULT '10',
  `delivery_fee` decimal(10,2) NOT NULL DEFAULT '0.00',
  `pay_method` tinyint DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `order_status` tinyint NOT NULL DEFAULT '10',
  `delivery_status` tinyint DEFAULT '0',
  `delivery_address` varchar(255) NOT NULL,
  `delivery_lat` decimal(10,7) DEFAULT NULL,
  `delivery_lng` decimal(10,7) DEFAULT NULL,
  `receiver_name` varchar(64) NOT NULL,
  `receiver_phone` varchar(20) NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `cancel_time` datetime DEFAULT NULL,
  `cancel_reason` varchar(255) DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `refund_id` bigint DEFAULT NULL,
  `coupon_id` bigint DEFAULT NULL,
  `coupon_discount` decimal(10,2) DEFAULT '0.00',
  `platform_subsidy` decimal(10,2) DEFAULT '0.00',
  `lock_stock_time` datetime DEFAULT NULL,
  `auto_cancel_time` datetime DEFAULT NULL,
  `channel` varchar(16) DEFAULT 'PC',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_driver_id` (`delivery_driver_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DELIMITER ;;
CREATE TRIGGER `trg_orders_status_insert` AFTER INSERT ON `orders` FOR EACH ROW INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, source) SELECT UUID_SHORT(), NEW.id, NEW.order_no, NULL, NEW.order_status, 'order_insert' WHERE NEW.order_status IS NOT NULL;;
DELIMITER ;
DELIMITER ;;
CREATE TRIGGER `trg_orders_status_update` AFTER UPDATE ON `orders` FOR EACH ROW INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, reason, source) SELECT UUID_SHORT(), NEW.id, NEW.order_no, OLD.order_status, NEW.order_status, COALESCE(NEW.cancel_reason, NULL), 'order_update' WHERE NOT (OLD.order_status <=> NEW.order_status);;
DELIMITER ;
DROP TABLE IF EXISTS `payment_log`;
CREATE TABLE `payment_log` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `order_no` varchar(32) NOT NULL,
  `pay_method` tinyint NOT NULL,
  `transaction_no` varchar(128) DEFAULT NULL,
  `pay_amount` decimal(10,2) NOT NULL,
  `pay_status` tinyint DEFAULT '10',
  `request_params` json DEFAULT NULL,
  `callback_response` json DEFAULT NULL,
  `refund_no` varchar(64) DEFAULT NULL,
  `refund_amount` decimal(10,2) DEFAULT NULL,
  `refund_time` datetime DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `platform_transaction`;
CREATE TABLE `platform_transaction` (
  `id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `transaction_no` varchar(64) NOT NULL,
  `direction` tinyint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `type` tinyint NOT NULL,
  `source` varchar(64) DEFAULT NULL,
  `balance_before` decimal(10,2) DEFAULT NULL,
  `balance_after` decimal(10,2) DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `confirm_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_type_status` (`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `refund_application`;
CREATE TABLE `refund_application` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `refund_amount` decimal(10,2) NOT NULL,
  `refund_reason` varchar(255) NOT NULL,
  `refund_desc` varchar(500) DEFAULT NULL,
  `evidence_images` json DEFAULT NULL,
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `audit_status` tinyint DEFAULT '10',
  `audit_operator_id` bigint DEFAULT NULL,
  `audit_remark` varchar(255) DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `refund_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `report_statistics`;
CREATE TABLE `report_statistics` (
  `id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `stat_type` varchar(32) NOT NULL,
  `stat_data` json NOT NULL,
  `merchant_id` bigint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_type_merchant` (`stat_date`,`stat_type`,`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `driver_id` bigint DEFAULT NULL,
  `dish_id` bigint DEFAULT NULL,
  `rating_food` int DEFAULT NULL,
  `rating_delivery` int DEFAULT NULL,
  `rating_service` int DEFAULT NULL,
  `overall_rating` decimal(2,1) NOT NULL,
  `content` varchar(500) DEFAULT NULL,
  `images` json DEFAULT NULL,
  `is_anonymous` tinyint(1) DEFAULT '0',
  `merchant_reply` varchar(500) DEFAULT NULL,
  `merchant_reply_time` datetime DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `rider_location`;
CREATE TABLE `rider_location` (
  `id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `driver_id` bigint NOT NULL,
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `item_type` tinyint NOT NULL,
  `dish_id` bigint DEFAULT NULL,
  `combo_id` bigint DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `selected` tinyint(1) DEFAULT '1',
  `customization_json` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`,`item_type`,`dish_id`,`combo_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL,
  `dict_type` varchar(64) NOT NULL,
  `dict_label` varchar(64) NOT NULL,
  `dict_value` tinyint NOT NULL,
  `dict_sort` int DEFAULT '0',
  `css_class` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT '10',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_value` (`dict_type`,`dict_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL,
  `username` varchar(64) NOT NULL,
  `password` varchar(128) NOT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(64) DEFAULT NULL,
  `role_type` tinyint NOT NULL,
  `status` tinyint DEFAULT '10',
  `register_source` tinyint DEFAULT '10',
  `last_login_time` datetime DEFAULT NULL,
  `last_login_ip` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_role_status` (`role_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL,
  `config_key` varchar(64) NOT NULL,
  `config_value` text NOT NULL,
  `config_group` varchar(64) DEFAULT 'default',
  `description` varchar(255) DEFAULT NULL,
  `is_sensitive` tinyint(1) DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `system_notice`;
CREATE TABLE `system_notice` (
  `id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `content` text NOT NULL,
  `notice_type` tinyint DEFAULT '10',
  `target_role` tinyint DEFAULT '0',
  `priority` int DEFAULT '0',
  `status` tinyint DEFAULT '10',
  `publish_time` datetime DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `receiver_name` varchar(64) NOT NULL,
  `receiver_phone` varchar(20) NOT NULL,
  `province` varchar(32) DEFAULT NULL,
  `city` varchar(32) DEFAULT NULL,
  `district` varchar(32) DEFAULT NULL,
  `detail_address` varchar(255) NOT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `address_tag` varchar(32) DEFAULT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_browse_history`;
CREATE TABLE `user_browse_history` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `item_type` tinyint NOT NULL,
  `item_id` bigint NOT NULL,
  `browse_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item` (`item_type`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `template_id` bigint NOT NULL,
  `coupon_code` varchar(32) NOT NULL,
  `status` tinyint DEFAULT '10',
  `used_time` datetime DEFAULT NULL,
  `used_order_id` bigint DEFAULT NULL,
  `expire_time` datetime NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_expire` (`status`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_membership`;
CREATE TABLE `user_membership` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `membership_type` tinyint NOT NULL,
  `status` tinyint DEFAULT '10',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `pay_amount` decimal(10,2) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status_end_time` (`status`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_plan`;
CREATE TABLE `user_plan` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `plan_days` int NOT NULL DEFAULT '7',
  `goal` varchar(64) DEFAULT NULL,
  `activity_level` tinyint DEFAULT NULL,
  `target_weight` decimal(5,2) DEFAULT NULL,
  `start_weight` decimal(5,2) DEFAULT NULL,
  `prefs` json DEFAULT NULL,
  `avoid` json DEFAULT NULL,
  `focus_parts` json DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '10',
  `cur_day` int NOT NULL DEFAULT '0',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `started_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_user_created` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_plan_meal`;
CREATE TABLE `user_plan_meal` (
  `id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `day_index` int NOT NULL,
  `meal_index` tinyint NOT NULL,
  `dish_id` bigint NOT NULL,
  `swap_count` int NOT NULL DEFAULT '0',
  `checked` tinyint(1) NOT NULL DEFAULT '0',
  `checked_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_plan_day` (`plan_id`,`day_index`,`meal_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `age` int DEFAULT NULL,
  `gender` tinyint DEFAULT NULL,
  `height` decimal(5,2) DEFAULT NULL,
  `weight` decimal(5,2) DEFAULT NULL,
  `activity_level` tinyint DEFAULT NULL,
  `diet_preference` json DEFAULT NULL,
  `allergy_info` json DEFAULT NULL,
  `disease_history` json DEFAULT NULL,
  `health_goal` varchar(64) DEFAULT NULL,
  `daily_calorie_target` int DEFAULT NULL,
  `target_weight` decimal(5,2) DEFAULT NULL,
  `exercise_freq` int DEFAULT NULL,
  `focus_parts` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_status_log`;
CREATE TABLE `user_status_log` (
  `id` bigint NOT NULL,
  `target_user_id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `old_status` tinyint DEFAULT NULL,
  `new_status` tinyint NOT NULL,
  `change_reason` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_target_user` (`target_user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `user_third_party_identity`;
CREATE TABLE `user_third_party_identity` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `provider` varchar(32) NOT NULL,
  `open_id` varchar(128) NOT NULL,
  `union_id` varchar(128) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_open_id` (`provider`,`open_id`),
  UNIQUE KEY `uk_user_provider` (`user_id`,`provider`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_third_party_identity_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `weight_record`;
CREATE TABLE `weight_record` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `record_date` date NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `body_fat_rate` decimal(5,2) DEFAULT NULL,
  `bmi` decimal(5,2) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`record_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `membership_payment_order`;
CREATE TABLE `membership_payment_order` (
  `id` bigint NOT NULL,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `pay_amount` decimal(10,2) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '10',
  `transaction_no` varchar(128) DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_payment_order_no` (`order_no`),
  KEY `idx_membership_payment_user` (`user_id`),
  KEY `idx_membership_payment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
