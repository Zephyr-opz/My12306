-- My12306 V2：动态座位区间库存与车次模板运行日期
-- 适用：已经执行基础结构和前序角色/订单结构迁移的 my12306 数据库。
-- 只新增字段、索引和表，不删除或清空既有业务数据。
USE `my12306`;

ALTER TABLE `trains`
    ADD COLUMN `run_start_date` DATE DEFAULT NULL COMMENT '模板生效开始日期' AFTER `arrival_time`,
    ADD COLUMN `run_end_date` DATE DEFAULT NULL COMMENT '模板生效结束日期' AFTER `run_start_date`;

UPDATE `trains` t
LEFT JOIN (
    SELECT `train_id`, MIN(`departure_date`) AS `min_date`, MAX(`departure_date`) AS `max_date`
    FROM `train_schedules`
    GROUP BY `train_id`
) s ON s.`train_id` = t.`id`
SET t.`run_start_date` = s.`min_date`,
    t.`run_end_date` = s.`max_date`
WHERE s.`train_id` IS NOT NULL;

ALTER TABLE `ticket_order_items`
    ADD COLUMN `schedule_seat_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运行计划座位ID' AFTER `schedule_id`,
    ADD KEY `idx_item_schedule_seat` (`schedule_id`, `schedule_seat_id`);

ALTER TABLE `tickets`
    ADD COLUMN `schedule_seat_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运行计划座位ID' AFTER `schedule_id`,
    ADD KEY `idx_ticket_schedule_seat` (`schedule_id`, `schedule_seat_id`);

CREATE TABLE `schedule_seats` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '运行计划座位主键',
    `schedule_id` BIGINT UNSIGNED NOT NULL COMMENT '每日运行计划ID',
    `train_id` BIGINT UNSIGNED NOT NULL COMMENT '车次模板ID',
    `carriage_number` VARCHAR(64) NOT NULL COMMENT '车厢号',
    `seat_number` VARCHAR(64) NOT NULL COMMENT '座位号',
    `seat_type` INT NOT NULL COMMENT '席别编码',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1可用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_carriage_seat` (`schedule_id`, `carriage_number`, `seat_number`),
    KEY `idx_schedule_seat_type` (`schedule_id`, `seat_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运行计划具体座位快照';

CREATE TABLE `seat_segment_occupancies` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '座位区间占用主键',
    `schedule_id` BIGINT UNSIGNED NOT NULL COMMENT '每日运行计划ID',
    `schedule_seat_id` BIGINT UNSIGNED NOT NULL COMMENT '运行计划座位ID',
    `order_sn` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    `ticket_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '车票ID',
    `start_sequence` INT NOT NULL COMMENT '出发站序号，含',
    `end_sequence` INT NOT NULL COMMENT '到达站序号，不含',
    `occupancy_status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已释放，1占用中',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `release_time` DATETIME DEFAULT NULL COMMENT '释放时间',
    PRIMARY KEY (`id`),
    KEY `idx_occupancy_schedule_seat` (`schedule_id`, `schedule_seat_id`, `occupancy_status`),
    KEY `idx_occupancy_order` (`order_sn`, `occupancy_status`),
    KEY `idx_occupancy_interval` (`schedule_id`, `start_sequence`, `end_sequence`, `occupancy_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运行计划座位区间占用';
