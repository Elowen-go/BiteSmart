-- 通过API注册的账号ID（雪花算法生成，每次不同）
-- admin(40): 350928603535511552 / admin123
-- merchant(20): 350928621742985216 / merchant123
-- user(10): 350928638813802496 / user123

USE bitesmart;

-- 商家信息
INSERT IGNORE INTO merchant (id, user_id, shop_name, shop_logo, business_license, license_number, contact_name, contact_phone, shop_address, status, audit_remark, create_time)
VALUES (20001, 350928621742985216, '轻食主义', NULL, 'license_123.pdf', '1234567890', '李四', '13800138001', '北京市朝阳区健康路123号', 20, '审核通过', NOW());

-- 菜品分类
INSERT IGNORE INTO dish_category (id, category_name, category_icon, sort_order, parent_id, create_time)
VALUES (1, '轻食沙拉', NULL, 1, 0, NOW()),
       (2, '主食套餐', NULL, 2, 0, NOW()),
       (3, '饮品', NULL, 3, 0, NOW());

-- 菜品
INSERT IGNORE INTO dish (id, merchant_id, category_id, dish_name, description, price, original_price, dish_image, calories, protein, fat, carbs, stock, lock_stock, status, create_time)
VALUES (1, 20001, 1, '凯撒沙拉', '经典凯撒沙拉，配鸡胸肉', 28.00, 35.00, NULL, 350, 25, 15, 20, 100, 0, 10, NOW()),
       (2, 20001, 1, '牛油果沙拉', '新鲜牛油果配蔬菜', 32.00, 40.00, NULL, 280, 18, 12, 30, 50, 0, 10, NOW()),
       (3, 20001, 2, '鸡胸肉套餐', '烤鸡胸肉配糙米和时蔬', 38.00, 45.00, NULL, 550, 45, 12, 45, 80, 0, 10, NOW()),
       (4, 20001, 2, '三文鱼套餐', '挪威三文鱼配藜麦', 48.00, 58.00, NULL, 620, 35, 25, 50, 30, 0, 10, NOW()),
       (5, 20001, 3, '鲜榨橙汁', '新鲜橙子现榨', 12.00, 15.00, NULL, 80, 1, 0, 20, 200, 0, 10, NOW());

-- 套餐
INSERT IGNORE INTO combo (id, merchant_id, combo_name, description, price, original_price, combo_image, total_calories, total_protein, total_fat, total_carbs, combo_type, status, sales_count, create_time)
VALUES (1, 20001, '减脂套餐A', '适合减脂期的营养套餐', 45.00, 58.00, NULL, 500, 40, 15, 40, 10, 10, 0, NOW()),
       (2, 20001, '增肌套餐B', '高蛋白增肌套餐', 55.00, 70.00, NULL, 700, 55, 20, 50, 20, 10, 0, NOW());

-- 套餐菜品关联
INSERT IGNORE INTO combo_dish_rel (id, combo_id, dish_id, quantity, is_fixed, create_time)
VALUES (1, 1, 1, 1, 1, NOW()),
       (2, 1, 3, 1, 1, NOW()),
       (3, 1, 5, 1, 0, NOW()),
       (4, 2, 4, 1, 1, NOW()),
       (5, 2, 5, 1, 0, NOW());

-- 订单
INSERT IGNORE INTO orders (id, order_no, user_id, merchant_id, total_amount, discount_amount, pay_amount, pay_method, order_status, delivery_status, delivery_address, create_time)
VALUES (1, 'ORD-20260710001', 350928638813802496, 20001, 66.00, 0.00, 66.00, 20, 50, 30, '北京市朝阳区幸福小区1号楼1001室', NOW()),
       (2, 'ORD-20260710002', 350928638813802496, 20001, 45.00, 0.00, 45.00, 10, 40, 20, '北京市朝阳区幸福小区1号楼1001室', NOW());

-- 订单明细
INSERT IGNORE INTO order_item (id, order_id, item_type, dish_id, snapshot_name, snapshot_price, quantity, sub_total, create_time)
VALUES (1, 1, 10, 1, '凯撒沙拉', 28.00, 1, 28.00, NOW()),
       (2, 1, 10, 3, '鸡胸肉套餐', 38.00, 1, 38.00, NOW()),
       (3, 2, 10, 1, '凯撒沙拉', 28.00, 1, 28.00, NOW()),
       (4, 2, 10, 5, '鲜榨橙汁', 12.00, 1, 12.00, NOW());

-- 评价
INSERT IGNORE INTO review (id, order_id, user_id, merchant_id, overall_rating, content, images, status, create_time)
VALUES (1, 1, 350928638813802496, 20001, 5.0, '非常美味，健康又好吃！', NULL, 10, NOW());

-- 会员套餐
INSERT IGNORE INTO membership_plan (id, plan_name, plan_type, price, original_price, valid_days, benefits, status, sort_order, create_time)
VALUES (1, '月卡会员', 10, 29.90, 39.90, 30, '{"ai_advanced":true,"exclusive_combo":true,"discount":0.95}', 10, 1, NOW()),
       (2, '季卡会员', 20, 79.90, 119.70, 90, '{"ai_advanced":true,"exclusive_combo":true,"discount":0.9,"priority_delivery":true}', 10, 2, NOW()),
       (3, '年卡会员', 30, 259.00, 478.80, 365, '{"ai_advanced":true,"exclusive_combo":true,"discount":0.85,"priority_delivery":true,"free_delivery":true}', 10, 3, NOW());

-- 用户地址
INSERT IGNORE INTO user_address (id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default, create_time)
VALUES (1, 350928638813802496, '张三', '13800138002', '北京市', '北京市', '朝阳区', '幸福小区1号楼1001室', 1, NOW());
