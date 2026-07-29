-- ============================================================
-- 数据库：健康膳食管理平台 V2.0（完整企业级版）
-- 字符集：utf8mb4
-- 引擎：InnoDB
-- 总表数：43张
-- 
-- 本脚本包含：
--   - 原始36张核心表
--   - 新增7张表（报表统计、优惠券模板、用户优惠券、投诉工单、
--     菜品营养独立、操作日志详情、平台资金流水）
--   - 3张表的结构增强（ALTER语句）
-- ============================================================

-- 创建数据库（如已存在请跳过）
CREATE DATABASE IF NOT EXISTS `bitesmart` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `bitesmart`;

-- ============================================================
-- 一、用户与权限域（10张表）
-- ============================================================

-- 1. 系统用户表
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL COMMENT '主键ID（雪花算法）',
  `username` varchar(64) NOT NULL COMMENT '登录账号（手机号/用户名）',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `role_type` tinyint(4) NOT NULL COMMENT '角色类型：10-普通用户 20-商家 30-配送员 40-管理员',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-正常 20-冻结 30-注销',
  `register_source` tinyint(4) DEFAULT 10 COMMENT '注册来源：10-PC 20-小程序 30-后台添加',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_role_status` (`role_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 用户健康档案表
CREATE TABLE `user_profile` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `age` int(4) DEFAULT NULL COMMENT '年龄',
  `gender` tinyint(4) DEFAULT NULL COMMENT '性别：10-男 20-女',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高（cm）',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重（kg）',
  `activity_level` tinyint(4) DEFAULT NULL COMMENT '运动量等级：10-久坐 20-轻度 30-中度 40-重度',
  `diet_preference` json DEFAULT NULL COMMENT '饮食偏好（JSON数组）如：["少盐","少油","素食"]',
  `allergy_info` json DEFAULT NULL COMMENT '过敏史（JSON数组）',
  `disease_history` json DEFAULT NULL COMMENT '疾病史（JSON数组）',
  `health_goal` varchar(64) DEFAULT NULL COMMENT '健康目标：减肥/增肌/维持/控糖/其他',
  `daily_calorie_target` int(8) DEFAULT NULL COMMENT '每日推荐摄入热量（由AI计算）',
  `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重（kg）',
  `exercise_freq` int(4) DEFAULT NULL COMMENT '每周运动频次（次）',
  `focus_parts` json DEFAULT NULL COMMENT '重点锻炼部位（JSON数组）如：["全身","腰腹"]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户健康档案表';

-- 3. 会员套餐定义表
/* CREATE TABLE `user_third_party_identity` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT 'BiteSmart用户ID',
  `provider` varchar(32) NOT NULL COMMENT '第三方平台：WECHAT_MINI_PROGRAM',
  `open_id` varchar(128) NOT NULL COMMENT '第三方平台用户标识',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信开放平台统一标识',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_open_id` (`provider`, `open_id`),
  UNIQUE KEY `uk_user_provider` (`user_id`, `provider`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_third_party_identity_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方身份绑定表';

*/
/* CREATE TABLE `user_third_party_identity` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT 'BiteSmart用户ID',
  `provider` varchar(32) NOT NULL COMMENT '第三方平台类型',
  `open_id` varchar(128) NOT NULL COMMENT '第三方平台用户标识',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信开放平台统一标识',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_open_id` (`provider`, `open_id`),
  UNIQUE KEY `uk_user_provider` (`user_id`, `provider`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_third_party_identity_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方身份绑定表';

*/
CREATE TABLE `user_third_party_identity` (
  `id` bigint(20) NOT NULL COMMENT 'primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'BiteSmart user id',
  `provider` varchar(32) NOT NULL COMMENT 'identity provider',
  `open_id` varchar(128) NOT NULL COMMENT 'provider open id',
  `union_id` varchar(128) DEFAULT NULL COMMENT 'provider union id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_open_id` (`provider`, `open_id`),
  UNIQUE KEY `uk_user_provider` (`user_id`, `provider`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_third_party_identity_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user third-party identity binding';

CREATE TABLE `membership_plan` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `plan_name` varchar(64) NOT NULL COMMENT '套餐名称（月卡/季卡/年卡）',
  `plan_type` tinyint(4) NOT NULL COMMENT '套餐类型：10-月卡 20-季卡 30-年卡',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价（展示折扣）',
  `valid_days` int(4) NOT NULL COMMENT '有效天数（月卡30天）',
  `benefits` json DEFAULT NULL COMMENT '权益描述（JSON）如：{"ai_advanced":true,"exclusive_combo":true,"discount":0.8}',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-上架 20-下架',
  `sort_order` int(4) DEFAULT 0 COMMENT '排序序号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`plan_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐定义表';

-- 4. 用户会员记录表
CREATE TABLE `user_membership` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `plan_id` bigint(20) NOT NULL COMMENT '关联会员套餐定义ID',
  `membership_type` tinyint(4) NOT NULL COMMENT '会员类型：10-月卡 20-季卡 30-年卡',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-生效中 20-已过期 30-已退款',
  `start_time` datetime NOT NULL COMMENT '生效开始时间',
  `end_time` datetime NOT NULL COMMENT '失效时间',
  `order_id` bigint(20) DEFAULT NULL COMMENT '关联购买订单ID',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '实际支付金额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status_end_time` (`status`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会员记录表';

-- 5. 商家信息表
CREATE TABLE `merchant` (
  `id` bigint(20) NOT NULL COMMENT '主键ID（关联sys_user.id）',
  `user_id` bigint(20) NOT NULL COMMENT '系统用户ID',
  `shop_name` varchar(128) NOT NULL COMMENT '店铺名称',
  `shop_logo` varchar(255) DEFAULT NULL COMMENT '店铺Logo',
  `business_license` varchar(255) DEFAULT NULL COMMENT '营业执照图片URL',
  `license_number` varchar(64) DEFAULT NULL COMMENT '营业执照编号',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `shop_address` varchar(255) DEFAULT NULL COMMENT '店铺地址',
  `shop_lat` decimal(10,7) DEFAULT NULL COMMENT '店铺纬度（GCJ-02）',
  `shop_lng` decimal(10,7) DEFAULT NULL COMMENT '店铺经度（GCJ-02）',
  `delivery_range` json DEFAULT NULL COMMENT '配送范围（JSON存多边形坐标或半径）',
  `business_hours` json DEFAULT NULL COMMENT '营业时间（JSON）如：{"monday":[{"start":"08:00","end":"22:00"}]}',
  `shop_notice` varchar(255) DEFAULT NULL COMMENT '店铺公告',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-待审核 20-审核通过 30-审核驳回 40-已关闭',
  `open_status` tinyint(4) NOT NULL DEFAULT 10 COMMENT '营业状态：10-营业中 20-打烊',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `avg_rating` decimal(2,1) DEFAULT 0.0 COMMENT '平均评分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_shop_name` (`shop_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家信息表';

-- 6. 商家入驻审核日志表
CREATE TABLE `merchant_audit_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `submit_time` datetime NOT NULL COMMENT '提交时间',
  `submit_data` json DEFAULT NULL COMMENT '提交的申请数据快照',
  `audit_operator_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_operator_name` varchar(64) DEFAULT NULL COMMENT '审核人姓名',
  `audit_status` tinyint(4) NOT NULL COMMENT '审核结果：10-待审核 20-通过 30-驳回',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_audit_time` (`audit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家入驻审核日志表';

-- 7. 配送员信息表
CREATE TABLE `delivery_driver` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '系统用户ID',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `vehicle_type` tinyint(4) DEFAULT 10 COMMENT '交通工具：10-电动车 20-自行车 30-汽车',
  `service_area` json DEFAULT NULL COMMENT '服务范围（JSON存区域编码数组）',
  `current_lat` decimal(10,7) DEFAULT NULL COMMENT '当前纬度',
  `current_lng` decimal(10,7) DEFAULT NULL COMMENT '当前经度',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-在线 20-忙碌 30-离线 40-冻结',
  `max_orders` int(4) DEFAULT 5 COMMENT '最大同时接单量',
  `current_orders` int(4) DEFAULT 0 COMMENT '当前在途订单数',
  `avg_rating` decimal(2,1) DEFAULT 0.0 COMMENT '平均评分',
  `total_deliveries` int(8) DEFAULT 0 COMMENT '累计配送单量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_current_location` (`current_lat`, `current_lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送员信息表';

-- 8. 配送员结算记录表
CREATE TABLE `driver_settlement` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `driver_id` bigint(20) NOT NULL COMMENT '配送员ID',
  `delivery_task_id` bigint(20) NOT NULL COMMENT '关联配送任务ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `delivery_fee` decimal(10,2) NOT NULL COMMENT '本单配送费',
  `bonus` decimal(10,2) DEFAULT 0.00 COMMENT '奖励/补贴金额',
  `penalty` decimal(10,2) DEFAULT 0.00 COMMENT '罚款/扣减金额',
  `settlement_amount` decimal(10,2) NOT NULL COMMENT '最终结算金额（配送费+奖励-罚款）',
  `settlement_status` tinyint(4) DEFAULT 10 COMMENT '结算状态：10-待结算 20-已结算 30-异常',
  `settlement_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_task_id` (`delivery_task_id`),
  KEY `idx_settlement_status` (`settlement_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送员结算记录表';

-- 9. 用户地址簿
CREATE TABLE `user_address` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货人电话',
  `province` varchar(32) DEFAULT NULL COMMENT '省份',
  `city` varchar(32) DEFAULT NULL COMMENT '城市',
  `district` varchar(32) DEFAULT NULL COMMENT '区/县',
  `detail_address` varchar(255) NOT NULL COMMENT '详细地址',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度（地图定位）',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `address_tag` varchar(32) DEFAULT NULL COMMENT '标签（家/公司/学校）',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否默认地址：1-是 0-否',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址簿';

-- 10. 用户状态变更审计日志表
CREATE TABLE `user_status_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `target_user_id` bigint(20) NOT NULL COMMENT '被操作用户ID',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID（管理员）',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `old_status` tinyint(4) DEFAULT NULL COMMENT '变更前状态',
  `new_status` tinyint(4) NOT NULL COMMENT '变更后状态',
  `change_reason` varchar(255) DEFAULT NULL COMMENT '变更原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_target_user` (`target_user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户状态变更审计日志表';


-- ============================================================
-- 二、健康记录域（3张表）
-- ============================================================

-- 11. 饮食记录表
CREATE TABLE `diet_record` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `record_time` time DEFAULT NULL COMMENT '记录时间',
  `meal_type` tinyint(4) DEFAULT 10 COMMENT '餐次：10-早餐 20-午餐 30-晚餐 40-加餐',
  `food_name` varchar(128) NOT NULL COMMENT '食物名称（自定义输入）',
  `quantity` int(4) NOT NULL DEFAULT 1 COMMENT '食用份数',
  `calories` int(8) DEFAULT 0 COMMENT '热量（大卡）',
  `protein` decimal(8,2) DEFAULT 0.00 COMMENT '蛋白质（g）',
  `fat` decimal(8,2) DEFAULT 0.00 COMMENT '脂肪（g）',
  `carbs` decimal(8,2) DEFAULT 0.00 COMMENT '碳水（g）',
  `source_type` tinyint(4) DEFAULT 20 COMMENT '来源：10-平台订单自动 20-用户手动添加 30-专属计划打卡',
  `order_item_id` bigint(20) DEFAULT NULL COMMENT '关联订单明细ID（自动导入时）',
  `plan_meal_id` bigint(20) DEFAULT NULL COMMENT '关联计划餐ID（source_type=30 时），取消打卡按此删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`),
  KEY `idx_plan_meal_id` (`plan_meal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录表';

-- 12. 运动记录表
CREATE TABLE `exercise_record` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `exercise_type` varchar(64) NOT NULL COMMENT '运动类型（跑步/游泳/健身/瑜伽等）',
  `duration` int(8) DEFAULT 0 COMMENT '运动时长（分钟）',
  `distance` decimal(8,2) DEFAULT 0.00 COMMENT '运动距离（公里）',
  `calories_burned` int(8) DEFAULT 0 COMMENT '消耗热量（大卡）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动记录表';

-- 13. 体重记录表
CREATE TABLE `weight_record` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '称重日期',
  `weight` decimal(5,2) NOT NULL COMMENT '体重（kg）',
  `body_fat_rate` decimal(5,2) DEFAULT NULL COMMENT '体脂率（%，可选）',
  `bmi` decimal(5,2) DEFAULT NULL COMMENT 'BMI指数（自动计算）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体重记录表';


-- ============================================================
-- 三、商品域（4张表）
-- ============================================================

-- 14. 菜品分类表
CREATE TABLE `dish_category` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称',
  `category_icon` varchar(255) DEFAULT NULL COMMENT '分类图标',
  `sort_order` int(4) DEFAULT 0 COMMENT '排序序号',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父级分类ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

-- 15. 菜品表
CREATE TABLE `dish` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `category_id` bigint(20) DEFAULT NULL COMMENT '分类ID',
  `dish_name` varchar(128) NOT NULL COMMENT '菜品名称',
  `dish_image` varchar(255) DEFAULT NULL COMMENT '菜品图片URL',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价（显示折扣）',
  `stock` int(8) NOT NULL DEFAULT 0 COMMENT '实际库存数量',
  `lock_stock` int(8) DEFAULT 0 COMMENT '锁定库存（下单未支付）',
  `min_stock_warning` int(8) DEFAULT 10 COMMENT '库存预警阈值',
  `sales_count` int(8) DEFAULT 0 COMMENT '总销量（含退款）',
  `sales_real` int(8) DEFAULT 0 COMMENT '真实销量（剔除退款）',
  `unit` varchar(16) DEFAULT '份' COMMENT '单位',
  `description` varchar(255) DEFAULT NULL COMMENT '菜品描述',
  `suitable_for` json DEFAULT NULL COMMENT '适宜人群：["减肥","增肌","控糖","儿童","老人"]',
  `tags` json DEFAULT NULL COMMENT '菜品标签（JSON数组）如：["高蛋白","减脂"]',
  `ai_comment` varchar(500) DEFAULT NULL COMMENT 'AI点评文案',
  `fit_scenes` json DEFAULT NULL COMMENT '适用场景（JSON数组）如：["减脂期","健身增肌"]',
  `cautions` json DEFAULT NULL COMMENT '忌口/注意事项（JSON数组）',
  `calories` int(8) DEFAULT 0 COMMENT '热量（大卡）',
  `protein` decimal(8,2) DEFAULT 0.00 COMMENT '蛋白质（g）',
  `fat` decimal(8,2) DEFAULT 0.00 COMMENT '脂肪（g）',
  `carbs` decimal(8,2) DEFAULT 0.00 COMMENT '碳水化合物（g）',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-上架 20-下架 30-售罄',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status_stock` (`status`, `stock`),
  KEY `idx_sales_real` (`sales_real`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- 16. 套餐表
CREATE TABLE `combo` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `combo_name` varchar(128) NOT NULL COMMENT '套餐名称',
  `combo_image` varchar(255) DEFAULT NULL COMMENT '套餐封面图',
  `price` decimal(10,2) NOT NULL COMMENT '套餐售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '套餐原价',
  `combo_type` tinyint(4) DEFAULT 10 COMMENT '套餐类型：10-减脂套餐 20-增肌套餐 30-控糖套餐 40-会员专属',
  `suitable_for` json DEFAULT NULL COMMENT '适宜人群',
  `total_calories` int(8) DEFAULT 0 COMMENT '套餐总热量',
  `total_protein` decimal(8,2) DEFAULT 0.00 COMMENT '套餐总蛋白质',
  `total_fat` decimal(8,2) DEFAULT 0.00 COMMENT '套餐总脂肪',
  `total_carbs` decimal(8,2) DEFAULT 0.00 COMMENT '套餐总碳水',
  `description` varchar(255) DEFAULT NULL COMMENT '套餐描述',
  `replaceable_dish_pool` json DEFAULT NULL COMMENT '可替换菜品池（菜品ID数组）',
  `max_replace_count` int(4) DEFAULT 0 COMMENT '最大可替换菜品数量（0表示不可换）',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-上架 20-下架',
  `sales_count` int(8) DEFAULT 0 COMMENT '套餐销量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_type_status` (`combo_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

-- 17. 套餐关联菜品关系表
CREATE TABLE `combo_dish_rel` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `combo_id` bigint(20) NOT NULL COMMENT '套餐ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `quantity` int(4) DEFAULT 1 COMMENT '该菜品在套餐中的份数',
  `is_fixed` tinyint(1) DEFAULT 1 COMMENT '是否固定不可替换：1-固定 0-可替换',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_combo_dish` (`combo_id`, `dish_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐关联菜品关系表';


-- ============================================================
-- 四、订单与支付域（6张表）
-- ============================================================

-- 18. 购物车表
CREATE TABLE `shopping_cart` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `item_type` tinyint(4) NOT NULL COMMENT '商品类型：10-菜品 20-套餐',
  `dish_id` bigint(20) DEFAULT NULL COMMENT '菜品ID',
  `combo_id` bigint(20) DEFAULT NULL COMMENT '套餐ID',
  `quantity` int(4) NOT NULL DEFAULT 1 COMMENT '数量',
  `selected` tinyint(1) DEFAULT 1 COMMENT '是否选中：1-选中 0-未选',
  `customization_json` json DEFAULT NULL COMMENT '定制信息（如套餐换菜）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_type`, `dish_id`, `combo_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 19. 订单主表
CREATE TABLE `orders` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号（唯一）',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `delivery_driver_id` bigint(20) DEFAULT NULL COMMENT '配送员ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` decimal(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '实付金额',
  `pay_method` tinyint(4) DEFAULT NULL COMMENT '支付方式：10-支付宝 20-微信',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `order_status` tinyint(4) NOT NULL DEFAULT 10 COMMENT '订单状态：10-待支付 20-待接单 30-备餐中 40-配送中 50-已完成 60-已取消 70-退款中 80-已退款',
  `delivery_status` tinyint(4) DEFAULT 0 COMMENT '配送状态：0-未配送 10-待取餐 20-已取餐 30-配送中 40-已送达',
  `delivery_address` varchar(255) NOT NULL COMMENT '配送地址',
  `delivery_lat` decimal(10,7) DEFAULT NULL COMMENT '收货地址纬度（GCJ-02）',
  `delivery_lng` decimal(10,7) DEFAULT NULL COMMENT '收货地址经度（GCJ-02）',
  `receiver_name` varchar(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货人电话',
  `remark` varchar(255) DEFAULT NULL COMMENT '订单备注',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `refund_id` bigint(20) DEFAULT NULL COMMENT '关联退款工单ID',
  `coupon_id` bigint(20) DEFAULT NULL COMMENT '使用的用户优惠券ID',
  `coupon_discount` decimal(10,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  `platform_subsidy` decimal(10,2) DEFAULT 0.00 COMMENT '平台补贴金额',
  `lock_stock_time` datetime DEFAULT NULL COMMENT '锁定库存时间（用于超时释放）',
  `auto_cancel_time` datetime DEFAULT NULL COMMENT '自动取消时间（支付超时）',
  `channel` varchar(16) DEFAULT 'PC' COMMENT '下单渠道：PC/小程序/H5',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_driver_id` (`delivery_driver_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 20. 订单明细表（快照存储）
CREATE TABLE `order_item` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `item_type` tinyint(4) NOT NULL COMMENT '商品类型：10-菜品 20-套餐',
  `dish_id` bigint(20) DEFAULT NULL COMMENT '菜品ID（冗余用于统计）',
  `combo_id` bigint(20) DEFAULT NULL COMMENT '套餐ID（冗余用于统计）',
  `snapshot_name` varchar(128) NOT NULL COMMENT '快照名称',
  `snapshot_image` varchar(255) DEFAULT NULL COMMENT '快照图片',
  `snapshot_price` decimal(10,2) NOT NULL COMMENT '快照单价',
  `snapshot_calories` int(8) DEFAULT 0 COMMENT '快照热量',
  `snapshot_protein` decimal(8,2) DEFAULT 0.00 COMMENT '快照蛋白质',
  `snapshot_fat` decimal(8,2) DEFAULT 0.00 COMMENT '快照脂肪',
  `snapshot_carbs` decimal(8,2) DEFAULT 0.00 COMMENT '快照碳水',
  `snapshot_nutrition_json` json DEFAULT NULL COMMENT '完整营养JSON快照',
  `quantity` int(4) NOT NULL DEFAULT 1 COMMENT '数量',
  `sub_total` decimal(10,2) NOT NULL COMMENT '小计金额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_combo_id` (`combo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 21. 支付流水表
CREATE TABLE `payment_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号',
  `pay_method` tinyint(4) NOT NULL COMMENT '支付方式：10-支付宝 20-微信',
  `transaction_no` varchar(128) DEFAULT NULL COMMENT '第三方交易流水号',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_status` tinyint(4) DEFAULT 10 COMMENT '支付状态：10-待支付 20-支付成功 30-支付失败 40-退款中 50-退款成功 60-退款失败',
  `request_params` json DEFAULT NULL COMMENT '请求参数快照',
  `callback_response` json DEFAULT NULL COMMENT '支付回调原始数据',
  `refund_no` varchar(64) DEFAULT NULL COMMENT '退款流水号',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `expire_time` datetime DEFAULT NULL COMMENT '支付超时时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- 22. 退款工单表
CREATE TABLE `refund_application` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号',
  `user_id` bigint(20) NOT NULL COMMENT '申请人用户ID',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '申请退款金额',
  `refund_reason` varchar(255) NOT NULL COMMENT '退款原因',
  `refund_desc` varchar(500) DEFAULT NULL COMMENT '退款描述',
  `evidence_images` json DEFAULT NULL COMMENT '凭证图片（JSON数组）',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `audit_status` tinyint(4) DEFAULT 10 COMMENT '审核状态：10-待审核 20-审核通过 30-审核驳回 40-已退款',
  `audit_operator_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注/驳回理由',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `refund_time` datetime DEFAULT NULL COMMENT '实际退款完成时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款工单表';

-- 23. 库存变动流水表
CREATE TABLE `inventory_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `change_type` tinyint(4) NOT NULL COMMENT '变动类型：10-入库 20-出库 30-下单锁定 40-支付扣减 50-取消释放 60-退款恢复 70-盘盈 80-盘亏',
  `change_quantity` int(8) NOT NULL COMMENT '变动数量（正数增加，负数减少）',
  `before_stock` int(8) NOT NULL COMMENT '变动前库存',
  `after_stock` int(8) NOT NULL COMMENT '变动后库存',
  `before_lock_stock` int(8) DEFAULT 0 COMMENT '变动前锁定库存',
  `after_lock_stock` int(8) DEFAULT 0 COMMENT '变动后锁定库存',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型（order/cancel/refund等）',
  `biz_id` bigint(20) DEFAULT NULL COMMENT '关联业务ID（订单ID等）',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动流水表';


-- ============================================================
-- 五、配送与物流域（1张表）
-- ============================================================

-- 24. 配送任务表
CREATE TABLE `delivery_task` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号',
  `driver_id` bigint(20) DEFAULT NULL COMMENT '配送员ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `merchant_address` varchar(255) NOT NULL COMMENT '商家取餐地址',
  `merchant_phone` varchar(20) NOT NULL COMMENT '商家联系电话',
  `delivery_address` varchar(255) NOT NULL COMMENT '用户收货地址',
  `merchant_lat` decimal(10,7) DEFAULT NULL COMMENT '商家取货点纬度（GCJ-02）',
  `merchant_lng` decimal(10,7) DEFAULT NULL COMMENT '商家取货点经度（GCJ-02）',
  `delivery_lat` decimal(10,7) DEFAULT NULL COMMENT '收货点纬度（GCJ-02）',
  `delivery_lng` decimal(10,7) DEFAULT NULL COMMENT '收货点经度（GCJ-02）',
  `receiver_name` varchar(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货人电话',
  `pickup_code` varchar(16) DEFAULT NULL COMMENT '取餐码',
  `task_status` tinyint(4) DEFAULT 10 COMMENT '任务状态：10-待接单 20-待取餐 30-已取餐 40-配送中 50-已送达 60-异常 70-已取消',
  `current_lat` decimal(10,7) DEFAULT NULL COMMENT '当前纬度',
  `current_lng` decimal(10,7) DEFAULT NULL COMMENT '当前经度',
  `location_update_time` datetime DEFAULT NULL COMMENT '位置更新时间',
  `pickup_time` datetime DEFAULT NULL COMMENT '取餐时间',
  `deliver_time` datetime DEFAULT NULL COMMENT '送达时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '预计送达时间',
  `route_json` json DEFAULT NULL COMMENT '配送路线（经纬度点数组）',
  `exception_reason` varchar(255) DEFAULT NULL COMMENT '异常原因',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '骑手拒单原因',
  `order_remark` varchar(500) DEFAULT NULL COMMENT '订单备注快照（创建任务时从订单拷贝）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_current_location` (`current_lat`, `current_lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送任务表';


-- ============================================================
-- 六、评价与AI交互域（2张表）
-- ============================================================

-- 25. 评价表
CREATE TABLE `review` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `driver_id` bigint(20) DEFAULT NULL COMMENT '配送员ID',
  `dish_id` bigint(20) DEFAULT NULL COMMENT '菜品ID（可空）',
  `rating_food` int(4) DEFAULT NULL COMMENT '菜品评分（1-5）',
  `rating_delivery` int(4) DEFAULT NULL COMMENT '配送评分（1-5）',
  `rating_service` int(4) DEFAULT NULL COMMENT '服务评分（1-5）',
  `overall_rating` decimal(2,1) NOT NULL COMMENT '综合评分（1-5）',
  `content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `images` json DEFAULT NULL COMMENT '评价图片（JSON数组）',
  `is_anonymous` tinyint(1) DEFAULT 0 COMMENT '是否匿名：1-匿名 0-实名',
  `merchant_reply` varchar(500) DEFAULT NULL COMMENT '商家回复内容',
  `merchant_reply_time` datetime DEFAULT NULL COMMENT '商家回复时间',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-已发布 20-已隐藏 30-违规删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 26. AI对话记录表
CREATE TABLE `ai_conversation` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID（用于多轮对话关联）',
  `question` text NOT NULL COMMENT '用户提问',
  `answer` text NOT NULL COMMENT 'AI回复内容',
  `model_name` varchar(64) DEFAULT 'qwen-plus' COMMENT '使用的模型名称',
  `prompt_tokens` int(8) DEFAULT 0 COMMENT '输入Token数',
  `completion_tokens` int(8) DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` int(8) DEFAULT 0 COMMENT '总Token数',
  `cost` decimal(10,6) DEFAULT 0.000000 COMMENT '预估费用（元）',
  `response_time` int(8) DEFAULT NULL COMMENT '响应耗时（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';


-- ============================================================
-- 七、系统运维与配置域（9张表）
-- ============================================================

-- 27. 系统配置表
CREATE TABLE `system_config` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `config_key` varchar(64) NOT NULL COMMENT '配置键（唯一）',
  `config_value` text NOT NULL COMMENT '配置值（JSON或字符串）',
  `config_group` varchar(64) DEFAULT 'default' COMMENT '配置分组',
  `description` varchar(255) DEFAULT NULL COMMENT '配置说明',
  `is_sensitive` tinyint(1) DEFAULT 0 COMMENT '是否敏感配置（前端不可见）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 28. 系统公告表
CREATE TABLE `system_notice` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `title` varchar(128) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `notice_type` tinyint(4) DEFAULT 10 COMMENT '公告类型：10-系统公告 20-健康知识 30-活动信息 40-升级通知',
  `target_role` tinyint(4) DEFAULT 0 COMMENT '目标角色：0-全部 10-用户 20-商家 30-配送员',
  `priority` int(4) DEFAULT 0 COMMENT '优先级（越大越靠前）',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-草稿 20-已发布 30-已下架',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 29. 消息通知表
CREATE TABLE `message_notification` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID',
  `msg_type` tinyint(4) DEFAULT 10 COMMENT '消息类型：10-订单通知 20-系统公告 30-活动推送 40-配送提醒',
  `title` varchar(128) NOT NULL COMMENT '消息标题',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `jump_url` varchar(255) DEFAULT NULL COMMENT '跳转链接',
  `is_read` tinyint(1) DEFAULT 0 COMMENT '是否已读：1-已读 0-未读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- 30. 操作日志表
CREATE TABLE `operation_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(64) DEFAULT NULL COMMENT '操作用户名',
  `role_type` tinyint(4) DEFAULT NULL COMMENT '用户角色',
  `operation` varchar(255) NOT NULL COMMENT '操作描述',
  `method` varchar(128) DEFAULT NULL COMMENT '请求方法（类名+方法名）',
  `request_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
  `request_params` json DEFAULT NULL COMMENT '请求参数',
  `request_ip` varchar(64) DEFAULT NULL COMMENT '请求IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '浏览器User-Agent',
  `cost_time` int(8) DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-成功 20-失败',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 31. 数据备份记录表
CREATE TABLE `data_backup_record` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `backup_name` varchar(128) NOT NULL COMMENT '备份名称',
  `backup_type` tinyint(4) DEFAULT 10 COMMENT '备份类型：10-全量 20-增量',
  `file_path` varchar(255) NOT NULL COMMENT '备份文件路径',
  `file_size` bigint(20) DEFAULT 0 COMMENT '文件大小（字节）',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-执行中 20-成功 30-失败',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据备份记录表';

-- 32. AI推荐规则配置表
CREATE TABLE `ai_recommend_rule` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `target_goal` varchar(64) NOT NULL COMMENT '目标人群（减肥/增肌/控糖/维持）',
  `prompt_template` text NOT NULL COMMENT 'Prompt模板（含变量占位符）',
  `calorie_float` int(4) DEFAULT 10 COMMENT '热量浮动百分比（±%）',
  `max_calories` int(8) DEFAULT NULL COMMENT '最高热量限制',
  `min_calories` int(8) DEFAULT NULL COMMENT '最低热量限制',
  `protein_ratio` decimal(5,2) DEFAULT NULL COMMENT '蛋白质供能比（%）',
  `fat_ratio` decimal(5,2) DEFAULT NULL COMMENT '脂肪供能比（%）',
  `carbs_ratio` decimal(5,2) DEFAULT NULL COMMENT '碳水供能比（%）',
  `model_version` varchar(32) DEFAULT 'v1.0' COMMENT '模型版本号',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-启用 20-停用',
  `priority` int(4) DEFAULT 0 COMMENT '优先级',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_goal_status` (`target_goal`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI推荐规则配置表';

-- 33. 营养膳食标准表
CREATE TABLE `nutrition_standard` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `standard_name` varchar(128) NOT NULL COMMENT '标准名称（如：成年男性减脂标准）',
  `gender` tinyint(4) DEFAULT NULL COMMENT '性别：10-男 20-女 0-不限',
  `age_min` int(4) DEFAULT NULL COMMENT '最小年龄',
  `age_max` int(4) DEFAULT NULL COMMENT '最大年龄',
  `activity_level` tinyint(4) DEFAULT NULL COMMENT '运动量等级：10-久坐 20-轻度 30-中度 40-重度',
  `target_goal` varchar(64) DEFAULT NULL COMMENT '目标（减肥/增肌/维持）',
  `daily_calories` int(8) NOT NULL COMMENT '每日推荐热量（大卡）',
  `protein_grams` decimal(8,2) NOT NULL COMMENT '每日推荐蛋白质（g）',
  `fat_grams` decimal(8,2) NOT NULL COMMENT '每日推荐脂肪（g）',
  `carbs_grams` decimal(8,2) NOT NULL COMMENT '每日推荐碳水（g）',
  `water_ml` int(8) DEFAULT NULL COMMENT '每日推荐饮水量（ml）',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-启用 20-停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_gender_age_activity` (`gender`, `age_min`, `age_max`, `activity_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营养膳食标准表';

-- 34. 系统字典表
CREATE TABLE `sys_dict_data` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dict_type` varchar(64) NOT NULL COMMENT '字典类型（如：order_status）',
  `dict_label` varchar(64) NOT NULL COMMENT '字典标签（显示名称）',
  `dict_value` tinyint(4) NOT NULL COMMENT '字典值（数字编码）',
  `dict_sort` int(4) DEFAULT 0 COMMENT '排序序号',
  `css_class` varchar(64) DEFAULT NULL COMMENT '样式类（用于前端颜色）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-启用 20-停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_value` (`dict_type`, `dict_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典表';

-- 35. 文件上传记录表
CREATE TABLE `file_upload_record` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '上传用户ID',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型（avatar/dish_image/license/review_image）',
  `biz_id` bigint(20) DEFAULT NULL COMMENT '关联业务ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_url` varchar(512) NOT NULL COMMENT '存储URL（OSS/CDN）',
  `file_size` bigint(20) DEFAULT 0 COMMENT '文件大小（字节）',
  `mime_type` varchar(64) DEFAULT NULL COMMENT '文件MIME类型',
  `storage_path` varchar(255) DEFAULT NULL COMMENT '存储服务内部路径',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录表';

-- 36. 用户浏览历史表（用于热门推荐统计）
CREATE TABLE `user_browse_history` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `item_type` tinyint(4) NOT NULL COMMENT '商品类型：10-菜品 20-套餐',
  `item_id` bigint(20) NOT NULL COMMENT '商品ID',
  `browse_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item` (`item_type`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览历史表';


-- ============================================================
-- 八、新增企业级扩展表（7张）
-- 包括：报表统计、优惠券体系、投诉工单、营养独立、操作详情、资金流水
-- ============================================================

-- 37. 报表统计表（预聚合，提升看板性能）
CREATE TABLE `report_statistics` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stat_type` varchar(32) NOT NULL COMMENT '统计类型：daily_sales/daily_orders/new_users/top_dishes',
  `stat_data` json NOT NULL COMMENT '统计数据（JSON）如：{"amount":1234.5, "count":50}',
  `merchant_id` bigint(20) DEFAULT 0 COMMENT '商家ID（0表示平台全局）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_type_merchant` (`stat_date`, `stat_type`, `merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表统计表（预聚合）';

-- 38. 优惠券模板表
CREATE TABLE `coupon_template` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `merchant_id` bigint(20) DEFAULT 0 COMMENT '发布商家（0为平台）',
  `coupon_name` varchar(128) NOT NULL COMMENT '优惠券名称（如：满30减5）',
  `type` tinyint(4) NOT NULL COMMENT '类型：10-满减券 20-折扣券 30-无门槛券',
  `value` decimal(10,2) NOT NULL COMMENT '优惠值（满减金额或折扣比例）',
  `threshold` decimal(10,2) DEFAULT 0.00 COMMENT '使用门槛（满多少元可用）',
  `total_quantity` int(8) DEFAULT 0 COMMENT '发行总量（0为不限）',
  `used_quantity` int(8) DEFAULT 0 COMMENT '已领取数量',
  `user_limit` int(4) DEFAULT 1 COMMENT '每人限领数量',
  `start_time` datetime NOT NULL COMMENT '生效时间',
  `expire_time` datetime NOT NULL COMMENT '失效时间',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-启用 20-停用 30-已过期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_time_status` (`start_time`, `expire_time`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 39. 用户优惠券表
CREATE TABLE `user_coupon` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `template_id` bigint(20) NOT NULL COMMENT '关联模板ID',
  `coupon_code` varchar(32) NOT NULL COMMENT '优惠券兑换码/编号',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-未使用 20-已使用 30-已过期 40-已冻结',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `used_order_id` bigint(20) DEFAULT NULL COMMENT '使用订单ID',
  `expire_time` datetime NOT NULL COMMENT '失效时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- 40. 投诉工单表
CREATE TABLE `complaint_ticket` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '关联订单ID',
  `user_id` bigint(20) NOT NULL COMMENT '投诉人ID',
  `target_type` tinyint(4) NOT NULL COMMENT '投诉对象：10-商家 20-配送员',
  `target_id` bigint(20) NOT NULL COMMENT '投诉对象ID',
  `complaint_reason` varchar(255) NOT NULL COMMENT '投诉原因',
  `complaint_desc` varchar(500) DEFAULT NULL COMMENT '投诉详情描述',
  `evidence_images` json DEFAULT NULL COMMENT '凭证图片',
  `status` tinyint(4) DEFAULT 10 COMMENT '处理状态：10-待处理 20-处理中 30-已完成 40-驳回',
  `admin_operator_id` bigint(20) DEFAULT NULL COMMENT '处理人ID',
  `admin_remark` varchar(255) DEFAULT NULL COMMENT '管理员处理备注',
  `result` varchar(255) DEFAULT NULL COMMENT '处理结果（如：已警告商家、赔付用户）',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉工单表';

-- 41. 菜品营养信息表（独立解耦，便于扩展）
CREATE TABLE `dish_nutrition` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID（唯一关联）',
  `calories` int(8) DEFAULT 0 COMMENT '热量（大卡）',
  `protein` decimal(8,2) DEFAULT 0.00 COMMENT '蛋白质（g）',
  `fat` decimal(8,2) DEFAULT 0.00 COMMENT '脂肪（g）',
  `carbs` decimal(8,2) DEFAULT 0.00 COMMENT '碳水化合物（g）',
  `fiber` decimal(8,2) DEFAULT 0.00 COMMENT '膳食纤维（g）',
  `sodium` decimal(8,2) DEFAULT 0.00 COMMENT '钠（mg）',
  `cholesterol` decimal(8,2) DEFAULT 0.00 COMMENT '胆固醇（mg）',
  `vitamin_a` decimal(8,2) DEFAULT 0.00 COMMENT '维生素A（μg）',
  `calcium` decimal(8,2) DEFAULT 0.00 COMMENT '钙（mg）',
  `iron` decimal(8,2) DEFAULT 0.00 COMMENT '铁（mg）',
  `nutrition_json` json DEFAULT NULL COMMENT '扩展营养数据（JSON）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品营养信息表（独立解耦）';

-- 42. 操作日志详情表（字段级变更记录，增强审计）
CREATE TABLE `operation_log_detail` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `log_id` bigint(20) NOT NULL COMMENT '关联操作日志ID',
  `field_name` varchar(64) NOT NULL COMMENT '变更字段名',
  `old_value` text COMMENT '变更前值',
  `new_value` text COMMENT '变更后值',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_id` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志详情表（字段级变更记录）';

-- 43. 平台资金流水表（财务对账专用）
CREATE TABLE `platform_transaction` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID',
  `transaction_no` varchar(64) NOT NULL COMMENT '交易流水号（唯一）',
  `direction` tinyint(4) NOT NULL COMMENT '资金方向：10-收入 20-支出',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `type` tinyint(4) NOT NULL COMMENT '交易类型：10-用户支付 20-商家结算 30-退款 40-提现 50-平台服务费',
  `source` varchar(64) DEFAULT NULL COMMENT '资金来源/去向描述',
  `balance_before` decimal(10,2) DEFAULT NULL COMMENT '变动前平台余额',
  `balance_after` decimal(10,2) DEFAULT NULL COMMENT '变动后平台余额',
  `status` tinyint(4) DEFAULT 10 COMMENT '状态：10-待确认 20-已确认 30-异常',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台资金流水表（财务对账）';


-- ============================================================
-- 补充表（20260718 专属计划模块 + 运动库）
-- 对应迁移脚本 docs/sql/20260718_plan_module.sql
-- ============================================================

-- 44. 用户专属计划表
CREATE TABLE `user_plan` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `plan_days` int(4) NOT NULL DEFAULT 7 COMMENT '计划天数（当前固定7天）',
  `goal` varchar(64) DEFAULT NULL COMMENT '健康目标快照（生成时从健康档案拷贝）',
  `activity_level` tinyint(4) DEFAULT NULL COMMENT '活动量等级快照：10-久坐 20-轻度 30-中度 40-重度',
  `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重（kg）',
  `start_weight` decimal(5,2) DEFAULT NULL COMMENT '开始体重（kg，生成时档案体重）',
  `prefs` json DEFAULT NULL COMMENT '饮食偏好快照（JSON数组）',
  `avoid` json DEFAULT NULL COMMENT '忌口快照（JSON数组）',
  `focus_parts` json DEFAULT NULL COMMENT '重点部位快照（JSON数组）',
  `status` tinyint(4) NOT NULL DEFAULT 10 COMMENT '状态：10-未开始 20-进行中 30-已完成 40-已取消',
  `cur_day` int(4) NOT NULL DEFAULT 0 COMMENT '当前进行到的天（0起，0表示第1天）',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `started_time` datetime DEFAULT NULL COMMENT '开始执行时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_user_created` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户专属计划表';

-- 45. 专属计划餐次表（7天 × 3餐 = 21条）
CREATE TABLE `user_plan_meal` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `plan_id` bigint(20) NOT NULL COMMENT '计划ID',
  `day_index` int(4) NOT NULL COMMENT '第几天（0起，与 user_plan.cur_day 对齐）',
  `meal_index` tinyint(4) NOT NULL COMMENT '餐次：0-早餐 1-午餐 2-晚餐',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品ID',
  `swap_count` int(4) NOT NULL DEFAULT 0 COMMENT '换菜次数（用于菜品池偏移重新选菜）',
  `checked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已打卡：0-未打卡 1-已打卡',
  `checked_time` datetime DEFAULT NULL COMMENT '打卡时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_plan_day` (`plan_id`, `day_index`, `meal_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专属计划餐次表';

-- 46. 运动库表
CREATE TABLE `exercise_library` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `category` varchar(32) NOT NULL COMMENT '分类：aerobic-有氧 strength-力量 shape-塑形 yoga-瑜伽',
  `name` varchar(64) NOT NULL COMMENT '运动名称',
  `std_text` varchar(64) DEFAULT NULL COMMENT '标准消耗描述，如 331kcal/5km',
  `kcal_per_min` decimal(6,2) DEFAULT NULL COMMENT '每分钟消耗热量（大卡）',
  `def_mins` int(4) DEFAULT 30 COMMENT '默认时长（分钟）',
  `def_dist` decimal(6,2) DEFAULT 0.00 COMMENT '默认距离（公里，无距离概念的运动为0）',
  `image_url` varchar(255) DEFAULT NULL COMMENT '封面图URL',
  `tags` json DEFAULT NULL COMMENT '标签（JSON数组）如：["全身","有氧","户外"]',
  `hot` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否常用：0-否 1-是',
  `sort` int(4) NOT NULL DEFAULT 0 COMMENT '排序序号，小的在前',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_category_sort` (`category`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动库表';

-- 47. 骑手配送轨迹点表
CREATE TABLE `rider_location` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '配送任务ID',
  `driver_id` bigint(20) NOT NULL COMMENT '骑手ID（delivery_driver.id）',
  `latitude` decimal(10,7) NOT NULL COMMENT '纬度（GCJ-02）',
  `longitude` decimal(10,7) NOT NULL COMMENT '经度（GCJ-02）',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手配送轨迹点表';

-- ============================================================
-- 执行完成
-- 共 47 张表
-- 表清单：
--   1. sys_user              2. user_profile
--   3. membership_plan       4. user_membership
--   5. merchant              6. merchant_audit_log
--   7. delivery_driver       8. driver_settlement
--   9. user_address          10. user_status_log
--   11. diet_record          12. exercise_record
--   13. weight_record        14. dish_category
--   15. dish                 16. combo
--   17. combo_dish_rel       18. shopping_cart
--   19. orders               20. order_item
--   21. payment_log          22. refund_application
--   23. inventory_log        24. delivery_task
--   25. review               26. ai_conversation
--   27. system_config        28. system_notice
--   29. message_notification 30. operation_log
--   31. data_backup_record   32. ai_recommend_rule
--   33. nutrition_standard   34. sys_dict_data
--   35. file_upload_record   36. user_browse_history
--   37. report_statistics    38. coupon_template
--   39. user_coupon          40. complaint_ticket
--   41. dish_nutrition       42. operation_log_detail
--   43. platform_transaction 44. user_plan
--   45. user_plan_meal       46. exercise_library
--   47. rider_location
-- ============================================================
