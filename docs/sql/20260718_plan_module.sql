-- ============================================================
-- 20260718 专属计划模块 + user_profile/dish 扩字段 + 运动库
-- 对应后端 /api/plan/* 与 /api/exercise/library
-- 执行前请先备份；ALTER 语句按"只执行一次"设计
-- ============================================================

USE bitesmart;

-- ------------------------------------------------------------
-- 1. user_profile 扩字段：目标体重 / 每周运动频次 / 重点部位
-- ------------------------------------------------------------
ALTER TABLE `user_profile`
    ADD COLUMN `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重（kg）' AFTER `daily_calorie_target`,
    ADD COLUMN `exercise_freq` int(4) DEFAULT NULL COMMENT '每周运动频次（次）' AFTER `target_weight`,
    ADD COLUMN `focus_parts` json DEFAULT NULL COMMENT '重点锻炼部位（JSON数组）如：["全身","腰腹"]' AFTER `exercise_freq`;

-- ------------------------------------------------------------
-- 2. dish 扩字段：标签 / AI点评 / 适用场景 / 忌口提醒
-- ------------------------------------------------------------
ALTER TABLE `dish`
    ADD COLUMN `tags` json DEFAULT NULL COMMENT '菜品标签（JSON数组）如：["高蛋白","减脂"]' AFTER `suitable_for`,
    ADD COLUMN `ai_comment` varchar(500) DEFAULT NULL COMMENT 'AI点评文案' AFTER `tags`,
    ADD COLUMN `fit_scenes` json DEFAULT NULL COMMENT '适用场景（JSON数组）如：["减脂期","健身增肌"]' AFTER `ai_comment`,
    ADD COLUMN `cautions` json DEFAULT NULL COMMENT '忌口/注意事项（JSON数组）' AFTER `fit_scenes`;

-- ------------------------------------------------------------
-- 3. diet_record：计划打卡关联（source_type 新增 30-计划餐打卡）
-- ------------------------------------------------------------
ALTER TABLE `diet_record`
    ADD COLUMN `plan_meal_id` bigint(20) DEFAULT NULL COMMENT '关联计划餐ID（source_type=30 时），取消打卡按此删除' AFTER `order_item_id`,
    ADD KEY `idx_plan_meal_id` (`plan_meal_id`),
    MODIFY COLUMN `source_type` tinyint(4) DEFAULT 20 COMMENT '来源：10-平台订单自动 20-用户手动添加 30-专属计划打卡';

-- ------------------------------------------------------------
-- 4. 专属计划主表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_plan` (
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

-- ------------------------------------------------------------
-- 5. 专属计划餐次表（7天 × 3餐 = 21条）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_plan_meal` (
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

-- ------------------------------------------------------------
-- 6. 运动库表 + 种子数据（对齐小程序原型 EX_LIB）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exercise_library` (
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

INSERT INTO `exercise_library` (`id`, `category`, `name`, `std_text`, `kcal_per_min`, `def_mins`, `def_dist`, `image_url`, `tags`, `hot`, `sort`) VALUES
(1, 'aerobic',  '户外跑步',   '331kcal/5km',    11.00, 30,  5.00,  'https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=400&q=80', '["全身","有氧","户外"]', 1, 1),
(2, 'aerobic',  '跑步机跑步', '248kcal/30分钟',  8.00, 30,  0.00,  'https://images.unsplash.com/photo-1576678927484-cc907957088c?w=400&q=80', '["全身","有氧","室内"]', 1, 2),
(3, 'aerobic',  '户外行走',   '162kcal/4km',     5.00, 40,  4.00,  'https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=400&q=80', '["低冲击","户外"]', 1, 3),
(4, 'aerobic',  '户外骑行',   '258kcal/10km',    8.00, 40, 10.00,  'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&q=80', '["下肢","有氧","户外"]', 0, 4),
(5, 'aerobic',  '游泳',       '330kcal/30分钟', 11.00, 30,  0.00,  'https://images.unsplash.com/photo-1530549387789-4c1017266635?w=400&q=80', '["全身","有氧","低冲击"]', 0, 5),
(6, 'strength', '力量训练',   '210kcal/30分钟',  7.00, 30,  0.00,  'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=400&q=80', '["力量","室内"]', 1, 6),
(7, 'shape',    '哑铃塑形',   '180kcal/30分钟',  6.00, 30,  0.00,  'https://images.unsplash.com/photo-1581009146145-b5ef050c2e30?w=400&q=80', '["塑形","上肢"]', 0, 7),
(8, 'yoga',     '瑜伽',       '120kcal/30分钟',  4.00, 30,  0.00,  'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&q=80', '["柔韧","舒缓"]', 0, 8)
ON DUPLICATE KEY UPDATE
  `category` = VALUES(`category`), `name` = VALUES(`name`), `std_text` = VALUES(`std_text`),
  `kcal_per_min` = VALUES(`kcal_per_min`), `def_mins` = VALUES(`def_mins`), `def_dist` = VALUES(`def_dist`),
  `image_url` = VALUES(`image_url`), `tags` = VALUES(`tags`), `hot` = VALUES(`hot`), `sort` = VALUES(`sort`);

-- ------------------------------------------------------------
-- 7. 12 道示例菜品种子数据（对齐小程序原型 DISHES 常量，
--    含 tags / ai_comment / fit_scenes / cautions 文案）
--    可重复执行：按主键 upsert
-- ------------------------------------------------------------
INSERT INTO `dish` (`id`, `merchant_id`, `category_id`, `dish_name`, `dish_image`, `price`, `original_price`,
                    `stock`, `min_stock_warning`, `sales_count`, `sales_real`, `unit`, `description`, `suitable_for`,
                    `tags`, `ai_comment`, `fit_scenes`, `cautions`,
                    `calories`, `protein`, `fat`, `carbs`, `status`) VALUES
(1, 20001, 1, '牛油果鸡胸肉沙拉', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&q=80', 29.00, NULL,
 36, 10, 824, 824, '份', '低温慢煮鸡胸 · 牛油果 · 混合生菜 · 油醋汁', '["减肥","增肌"]',
 '["高蛋白","减脂"]', '蛋白质占全天目标的 36%，牛油果提供优质不饱和脂肪，适合作为减脂期晚餐。搭配一杯无糖酸奶更均衡。', '["减脂期","健身增肌","控糖饮食"]', '["坚果过敏慎选","含少量乳制品"]',
 326, 32, 12, 18, 10),
(2, 20001, 1, '彩虹藜麦能量碗', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80', 33.00, NULL,
 42, 10, 657, 657, '份', '三色藜麦 · 烤时蔬 · 鹰嘴豆 · 芝麻酱', '["减肥","素食"]',
 '["全谷物","饱腹感强"]', '复合碳水释放平稳，下午不容易犯困。藜麦是完全蛋白，素食日也能吃够氨基酸。', '["素食","健身人群","午餐正餐"]', '["含芝麻"]',
 412, 18, 11, 52, 10),
(3, 20001, 2, '香煎三文鱼时蔬', 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=400&q=80', 36.00, NULL,
 25, 10, 931, 931, '份', '挪威三文鱼 · 芦笋 · 小番茄 · 柠檬黄油汁', '["减肥","控糖"]',
 '["Omega-3","低卡"]', 'Omega-3 帮助控制炎症，285 kcal 的晚餐优选。蛋白质 28g，正好补上你今天的缺口。', '["减脂期","控糖饮食","用脑人群"]', '["鱼类过敏慎选"]',
 285, 28, 14, 12, 10),
(4, 20001, 1, '田园时蔬温沙拉', 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=400&q=80', 22.00, NULL,
 50, 10, 412, 412, '份', '烤南瓜 · 西兰花 · 孢子甘蓝 · 蜂蜜芥末汁', '["减肥","素食"]',
 '["素食","轻负担"]', '热量不到 200 kcal，适合轻断食日晚餐。建议加一份鸡胸或鸡蛋补足蛋白质。', '["素食","轻断食日"]', '["蛋白质偏低，建议搭配"]',
 198, 9, 8, 24, 10),
(5, 20001, 2, '香草烤鲈鱼配时蔬', 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400&q=80', 39.00, NULL,
 18, 10, 386, 386, '份', '海鲈鱼 · 迷迭香 · 烤根茎蔬菜', '["增肌"]',
 '["高蛋白","低脂"]', '34g 蛋白质 + 低脂，增肌期的理想正餐。鲈鱼刺少，老人孩子也好入口。', '["增肌期","低碳饮食"]', '["鱼类过敏慎选"]',
 310, 34, 11, 15, 10),
(6, 20001, 2, '照烧鸡胸糙米饭', 'https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&q=80', 27.00, NULL,
 60, 10, 1102, 1102, '份', '照烧鸡腿去皮 · 糙米 · 溏心蛋 · 海苔', '["增肌"]',
 '["高蛋白","主食均衡"]', '练后餐首选：35g 蛋白质 + 48g 缓释碳水，肌肉修复与糖原补充一碗搞定。', '["健身增肌","午餐正餐"]', '["含鸡蛋","照烧汁含糖"]',
 445, 35, 9, 48, 10),
(7, 20001, 1, '鸡丝荞麦凉面', 'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400&q=80', 24.00, NULL,
 33, 10, 543, 543, '份', '荞麦面 · 手撕鸡胸 · 黄瓜丝 · 麻酱汁', '["减肥"]',
 '["低脂","夏日限定"]', '荞麦升糖指数低，麻酱控制在一勺以内，整碗只有 7g 脂肪。', '["夏季食欲差","控脂期"]', '["含麸质","含芝麻"]',
 368, 22, 7, 52, 10),
(8, 20001, 2, '泰式青咖喱鸡肉碗', 'https://images.unsplash.com/photo-1455619452474-d2be8b1e70cd?w=400&q=80', 31.00, NULL,
 22, 10, 298, 298, '份', '青咖喱 · 鸡腿肉 · 泰国香米（半份）· 九层塔', '["其他"]',
 '["异域风味"]', '想吃重口的日子选它：椰浆减半、香米半份，风味保留、热量比外卖同款低 40%。', '["想吃重口时","正常饮食日"]', '["微辣","含椰浆"]',
 398, 26, 16, 38, 10),
(9, 20001, 1, '蓝莓燕麦松饼', 'https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400&q=80', 19.00, NULL,
 28, 10, 476, 476, '份', '全麦松饼 · 新鲜蓝莓 · 无糖枫糖浆', '["减肥"]',
 '["早餐","低GI"]', '早餐碳水选低 GI 的全麦，蓝莓补花青素。268 kcal 开启稳定供能的一上午。', '["早餐","加餐"]', '["含麸质"]',
 268, 8, 7, 42, 10),
(10, 20001, 1, '希腊酸奶莓果杯', 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400&q=80', 16.00, NULL,
 45, 10, 689, 689, '份', '无糖希腊酸奶 · 混合莓果 · 格兰诺拉', '["减肥","增肌"]',
 '["加餐","高蛋白"]', '下午馋甜食时的救星：12g 蛋白质、无添加糖，满足感来自莓果本身的甜。', '["加餐","下午茶替代"]', '["含乳制品"]',
 182, 12, 5, 22, 10),
(11, 20001, 1, '法式全麦吐司配煎蛋', 'https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=400&q=80', 21.00, NULL,
 20, 10, 355, 355, '份', '全麦吐司 · 太阳蛋 · 牛油果泥 · 小番茄', '["增肌"]',
 '["早餐"]', '经典早餐组合，295 kcal + 14g 蛋白质，配黑咖啡就是完整的晨间仪式。', '["早餐","周末早午餐"]', '["含鸡蛋","含麸质"]',
 295, 14, 12, 33, 10),
(12, 20001, 1, '莓果思慕雪碗', 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=400&q=80', 23.00, NULL,
 31, 10, 264, 264, '份', '冻莓果 · 香蕉 · 燕麦奶 · 奇亚籽', '["减肥"]',
 '["早餐","抗氧化"]', '花青素 + 膳食纤维双高的一碗。奇亚籽带来 omega-3 与饱腹感。', '["早餐","轻断食日"]', '["果糖偏高，控糖者少量"]',
 236, 9, 4, 41, 10)
ON DUPLICATE KEY UPDATE
  `merchant_id` = VALUES(`merchant_id`), `category_id` = VALUES(`category_id`),
  `dish_name` = VALUES(`dish_name`), `dish_image` = VALUES(`dish_image`),
  `price` = VALUES(`price`), `stock` = VALUES(`stock`),
  `sales_count` = VALUES(`sales_count`), `sales_real` = VALUES(`sales_real`),
  `description` = VALUES(`description`), `suitable_for` = VALUES(`suitable_for`),
  `tags` = VALUES(`tags`), `ai_comment` = VALUES(`ai_comment`),
  `fit_scenes` = VALUES(`fit_scenes`), `cautions` = VALUES(`cautions`),
  `calories` = VALUES(`calories`), `protein` = VALUES(`protein`),
  `fat` = VALUES(`fat`), `carbs` = VALUES(`carbs`), `status` = VALUES(`status`);
