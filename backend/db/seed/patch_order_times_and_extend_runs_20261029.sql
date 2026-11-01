-- My12306 2026-10-29 数据修正：订单实际时间和模板运行日期扩展。
-- 可重复执行；不删除订单、车票、座位或支付数据。
USE `my12306`;
START TRANSACTION;

-- 历史订单若缺少下单时间，使用订单创建时间补齐；已有真实下单时间不覆盖。
UPDATE `ticket_orders`
SET `order_time` = `create_time`
WHERE `order_time` IS NULL AND `create_time` IS NOT NULL;

-- 将订单的模板基准日期换算为对应运行计划日期，保留下跨日运行偏移。
UPDATE `ticket_orders` o
JOIN `train_schedules` s ON s.`id` = o.`schedule_id`
JOIN `trains` t ON t.`id` = o.`train_id`
JOIN `train_stations` ds ON ds.`train_id` = o.`train_id` AND ds.`departure` = o.`departure`
JOIN `train_stations` ar ON ar.`train_id` = o.`train_id` AND ar.`arrival` = o.`arrival`
SET o.`departure_time` = TIMESTAMP(
        DATE_ADD(s.`departure_date`, INTERVAL DATEDIFF(DATE(ds.`departure_time`), DATE(t.`departure_time`)) DAY),
        TIME(ds.`departure_time`)
    ),
    o.`arrival_time` = TIMESTAMP(
        DATE_ADD(s.`departure_date`, INTERVAL DATEDIFF(DATE(ar.`arrival_time`), DATE(t.`departure_time`)) DAY),
        TIME(ar.`arrival_time`)
    )
WHERE o.`schedule_id` IS NOT NULL
  AND ds.`departure_time` IS NOT NULL
  AND ar.`arrival_time` IS NOT NULL;

-- 所有当前模板至少延长到 2026-11-10；更晚的运行结束日期不缩短。
UPDATE `trains`
SET `run_end_date` = '2026-11-10'
WHERE `run_end_date` IS NULL OR `run_end_date` < '2026-11-10';

-- 从 2026-11-05 的最后一份运行计划复制 11-06 至 11-10，保持固定车次时间和状态。
INSERT IGNORE INTO `train_schedules`
    (`train_id`, `departure_date`, `departure_time`, `arrival_time`, `sale_status`, `plan_status`, `remark`, `create_time`, `update_time`)
SELECT s.`train_id`,
       DATE_ADD(s.`departure_date`, INTERVAL d.`offset_days` DAY),
       DATE_ADD(s.`departure_time`, INTERVAL d.`offset_days` DAY),
       DATE_ADD(s.`arrival_time`, INTERVAL d.`offset_days` DAY),
       s.`sale_status`, s.`plan_status`, s.`remark`, NOW(), NOW()
FROM `train_schedules` s
JOIN (
    SELECT 1 AS `offset_days`
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) d
WHERE s.`departure_date` = '2026-11-05';

COMMIT;
