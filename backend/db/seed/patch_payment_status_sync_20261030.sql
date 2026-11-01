-- My12306 2026-10-30 数据修正：已支付订单同步订单明细和车票状态。
-- 可重复执行；不删除订单、车票或支付数据。
USE `my12306`;
START TRANSACTION;

UPDATE `ticket_order_items` i
JOIN `ticket_orders` o ON o.`order_sn` = i.`order_sn`
SET i.`status` = 10
WHERE o.`status` = 10 AND i.`status` <> 10;

UPDATE `tickets` t
JOIN `ticket_orders` o ON o.`order_sn` = t.`order_sn`
SET t.`ticket_status` = 10
WHERE o.`status` = 10 AND t.`ticket_status` <> 10;

COMMIT;
