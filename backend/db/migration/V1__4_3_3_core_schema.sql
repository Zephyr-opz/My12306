-- My12306 V1：4.3.3 前五模块和角色权限所需结构变更
-- 适用：已经执行 V0__baseline_schema.sql 的 my12306 数据库
-- 规则：只新增字段、索引和表，不删除、不清空、不重建已有业务数据。
-- 执行前请先完成 mysqldump 备份；本文件按 Flyway V1 迁移命名，后续迁移继续使用 V2、V3……

USE `my12306`;

-- 1. 用户角色与实名资料
ALTER TABLE `user_accounts`
    ADD COLUMN `real_name` VARCHAR(100) DEFAULT NULL COMMENT '真实姓名' AFTER `username`,
    ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN管理员，USER普通用户' AFTER `phone`,
    ADD KEY `idx_role_status_deleted` (`role`, `status`, `deleted`);

-- 保留现有账号和密码，仅把已有 admin 账号标为管理员；不存在时不插入新账号。
UPDATE `user_accounts`
SET `role` = 'ADMIN'
WHERE `username` = 'admin' AND `deleted` = 0;

-- 2. 运行计划增加计划状态和备注；原 sale_status 继续表示售票状态。
ALTER TABLE `train_schedules`
    ADD COLUMN `plan_status` TINYINT NOT NULL DEFAULT 1 COMMENT '计划状态：0停用，1启用' AFTER `sale_status`,
    ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT '计划备注' AFTER `plan_status`,
    ADD KEY `idx_schedule_plan_status` (`departure_date`, `plan_status`, `sale_status`);

-- 3. 订单补充运行计划、金额和失效/售后时间，不改变已有订单字段和状态值。
ALTER TABLE `ticket_orders`
    ADD COLUMN `schedule_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '每日运行计划ID' AFTER `train_id`,
    ADD COLUMN `total_amount` INT DEFAULT 0 COMMENT '订单总金额，单位分' AFTER `arrival_time`,
    ADD COLUMN `expire_time` DATETIME DEFAULT NULL COMMENT '待支付失效时间' AFTER `total_amount`,
    ADD COLUMN `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间' AFTER `expire_time`,
    ADD COLUMN `refund_time` DATETIME DEFAULT NULL COMMENT '退票时间' AFTER `cancel_time`,
    ADD KEY `idx_schedule_date_status` (`schedule_id`, `riding_date`, `status`);

ALTER TABLE `ticket_order_items`
    ADD COLUMN `schedule_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '每日运行计划ID' AFTER `train_id`,
    ADD COLUMN `riding_date` DATE DEFAULT NULL COMMENT '乘车日期' AFTER `schedule_id`,
    ADD COLUMN `departure` VARCHAR(64) DEFAULT NULL COMMENT '出发站' AFTER `riding_date`,
    ADD COLUMN `arrival` VARCHAR(64) DEFAULT NULL COMMENT '到达站' AFTER `departure`,
    ADD COLUMN `price` INT DEFAULT NULL COMMENT '单张票价，单位分' AFTER `amount`,
    ADD KEY `idx_item_schedule_date` (`schedule_id`, `riding_date`);

ALTER TABLE `tickets`
    ADD COLUMN `order_sn` VARCHAR(64) DEFAULT NULL COMMENT '订单号' AFTER `id`,
    ADD COLUMN `schedule_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '每日运行计划ID' AFTER `train_id`,
    ADD COLUMN `riding_date` DATE DEFAULT NULL COMMENT '乘车日期' AFTER `schedule_id`,
    ADD COLUMN `departure` VARCHAR(64) DEFAULT NULL COMMENT '出发站' AFTER `riding_date`,
    ADD COLUMN `arrival` VARCHAR(64) DEFAULT NULL COMMENT '到达站' AFTER `departure`,
    ADD COLUMN `seat_type` INT DEFAULT NULL COMMENT '席别' AFTER `seat_number`,
    ADD COLUMN `price` INT DEFAULT NULL COMMENT '票价，单位分' AFTER `seat_type`,
    ADD KEY `idx_ticket_order_sn` (`order_sn`),
    ADD KEY `idx_ticket_schedule_date` (`schedule_id`, `riding_date`);

-- 4. 以“运行计划 + 区间 + 席别”保存可售库存，支持后续购票扣减和退票回库。
CREATE TABLE `seat_inventories` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存主键',
    `schedule_id` BIGINT UNSIGNED NOT NULL COMMENT '每日运行计划ID',
    `train_id` BIGINT UNSIGNED NOT NULL COMMENT '车次ID',
    `departure_station` VARCHAR(256) NOT NULL COMMENT '区间出发站',
    `arrival_station` VARCHAR(256) NOT NULL COMMENT '区间到达站',
    `seat_type` INT NOT NULL COMMENT '席别编码',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '区间总席位数',
    `available_count` INT NOT NULL DEFAULT 0 COMMENT '区间可售席位数',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inventory_segment_seat` (`schedule_id`, `departure_station`, `arrival_station`, `seat_type`),
    KEY `idx_inventory_train_date` (`train_id`, `schedule_id`),
    KEY `idx_inventory_available` (`schedule_id`, `seat_type`, `available_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运行计划区间席位库存';

-- 5. 管理员增删改和计划维护的审计记录。
CREATE TABLE `admin_operation_logs` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `module` VARCHAR(50) NOT NULL COMMENT '业务模块',
    `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    `target_id` VARCHAR(64) DEFAULT NULL COMMENT '目标ID',
    `detail` JSON DEFAULT NULL COMMENT '操作摘要，不保存密码等敏感信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_log_operator_time` (`operator_id`, `create_time`),
    KEY `idx_admin_log_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计日志';

-- 6. 席别字典，业务表继续使用原有 int 编码，后端可通过本表显示名称。
CREATE TABLE `seat_types` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '席别主键',
    `code` VARCHAR(32) NOT NULL COMMENT '席别编码',
    `name` VARCHAR(64) NOT NULL COMMENT '席别名称',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seat_type_code` (`code`),
    KEY `idx_seat_type_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='席别字典';
