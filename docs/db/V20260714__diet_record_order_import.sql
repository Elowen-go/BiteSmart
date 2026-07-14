ALTER TABLE diet_record
    ADD COLUMN record_time time DEFAULT NULL COMMENT '记录时间' AFTER record_date,
    ADD COLUMN quantity int NOT NULL DEFAULT 1 COMMENT '食用份数' AFTER food_name;

CREATE UNIQUE INDEX uk_diet_record_order_item
    ON diet_record (order_item_id);

INSERT INTO weight_record (id, user_id, record_date, weight)
SELECT UUID_SHORT(), p.user_id, CURDATE(), p.weight
FROM user_profile p
WHERE p.deleted = 0
  AND p.weight IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM weight_record w
      WHERE w.user_id = p.user_id
        AND w.record_date = CURDATE()
        AND w.deleted = 0
  );
