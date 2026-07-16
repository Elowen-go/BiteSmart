-- Persist every order status transition for audit and operations timelines.
CREATE TABLE IF NOT EXISTS order_status_log (
    id BIGINT NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    from_status TINYINT NULL,
    to_status TINYINT NOT NULL,
    operator_id BIGINT NULL,
    operator_type VARCHAR(32) NULL,
    reason VARCHAR(255) NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'database_trigger',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_status_log_order (order_id, create_time),
    KEY idx_order_status_log_status (to_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, source)
SELECT UUID_SHORT(), o.id, o.order_no, NULL, o.order_status, 'backfill'
FROM orders o
LEFT JOIN order_status_log l ON l.order_id = o.id
WHERE o.deleted = 0 AND l.id IS NULL AND o.order_status IS NOT NULL;

-- These trigger statements are intentionally single-statement triggers so they can
-- be executed by migration runners without delimiter directives.
DROP TRIGGER IF EXISTS trg_orders_status_insert;
CREATE TRIGGER trg_orders_status_insert
AFTER INSERT ON orders
FOR EACH ROW
INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, source)
SELECT UUID_SHORT(), NEW.id, NEW.order_no, NULL, NEW.order_status, 'order_insert'
WHERE NEW.order_status IS NOT NULL;

DROP TRIGGER IF EXISTS trg_orders_status_update;
CREATE TRIGGER trg_orders_status_update
AFTER UPDATE ON orders
FOR EACH ROW
INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, reason, source)
SELECT UUID_SHORT(), NEW.id, NEW.order_no, OLD.order_status, NEW.order_status,
       NEW.cancel_reason, 'order_update'
WHERE NOT (OLD.order_status <=> NEW.order_status);
