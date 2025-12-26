-- ========================================
-- 云上重走长征路 - 数据库初始化脚本
-- Database: changzheng_db
-- Version: V1
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `changzheng_db` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `changzheng_db`;

-- ========================================
-- 1. 用户表 t_user
-- ========================================
CREATE TABLE `t_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `openid` VARCHAR(64) NOT NULL COMMENT '微信小程序openid',
    `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信unionid(可选)',
    `session_key` VARCHAR(128) DEFAULT NULL COMMENT '微信session_key(加密存储)',
    `student_no` VARCHAR(64) NOT NULL COMMENT '学号(AES加密存储)',
    `student_no_suffix` VARCHAR(4) NOT NULL COMMENT '学号后4位(用于展示)',
    `name` VARCHAR(64) DEFAULT NULL COMMENT '姓名(AES加密存储)',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '微信昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `class_id` VARCHAR(32) DEFAULT NULL COMMENT '班级编码',
    `class_name` VARCHAR(64) DEFAULT NULL COMMENT '班级名称',
    `grade` VARCHAR(16) DEFAULT NULL COMMENT '年级(如2022级)',
    `college` VARCHAR(64) DEFAULT NULL COMMENT '学院',
    `enroll_year` INT DEFAULT NULL COMMENT '入学年份',
    `total_mileage` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '累计里程(公里)',
    `total_steps` BIGINT NOT NULL DEFAULT 0 COMMENT '累计步数',
    `continuous_days` INT NOT NULL DEFAULT 0 COMMENT '当前连续运动天数',
    `max_continuous_days` INT NOT NULL DEFAULT 0 COMMENT '历史最大连续天数',
    `last_sync_date` DATE DEFAULT NULL COMMENT '最后同步日期',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0-学生 1-管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_class_id` (`class_id`),
    KEY `idx_grade` (`grade`),
    KEY `idx_total_mileage` (`total_mileage` DESC),
    KEY `idx_enroll_year` (`enroll_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 每日步数表 t_daily_steps
-- ========================================
CREATE TABLE `t_daily_steps` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `record_date` DATE NOT NULL COMMENT '记录日期',
    `raw_steps` INT NOT NULL DEFAULT 0 COMMENT '原始步数(微信返回)',
    `valid_steps` INT NOT NULL DEFAULT 0 COMMENT '有效步数(上限裁剪后)',
    `source` VARCHAR(32) NOT NULL DEFAULT 'WECHAT' COMMENT '数据来源: WECHAT/MANUAL/SYSTEM',
    `is_anomaly` TINYINT NOT NULL DEFAULT 0 COMMENT '是否异常: 0-正常 1-异常',
    `anomaly_reason` VARCHAR(128) DEFAULT NULL COMMENT '异常原因',
    `encrypted_data` TEXT DEFAULT NULL COMMENT '微信加密数据(备查)',
    `iv` VARCHAR(64) DEFAULT NULL COMMENT '微信加密iv',
    `signature` VARCHAR(128) DEFAULT NULL COMMENT '数据签名(防篡改)',
    `sync_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    KEY `idx_record_date` (`record_date`),
    KEY `idx_is_anomaly` (`is_anomaly`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日步数表';

-- ========================================
-- 3. 里程流水表 t_mileage_ledger (核心对账表)
-- ========================================
CREATE TABLE `t_mileage_ledger` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `record_date` DATE NOT NULL COMMENT '记录日期',
    `steps` INT NOT NULL DEFAULT 0 COMMENT '当日有效步数',
    `mileage_delta` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '里程增量(公里)',
    `mileage_before` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '变更前累计里程',
    `mileage_after` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '变更后累计里程',
    `conversion_rate` INT NOT NULL DEFAULT 2000 COMMENT '换算比例(步/公里)',
    `daily_limit` INT NOT NULL DEFAULT 30000 COMMENT '当日上限步数',
    `reason` VARCHAR(64) NOT NULL DEFAULT 'DAILY_SYNC' COMMENT '变更原因: DAILY_SYNC/MANUAL_FIX/RECALC',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-作废 1-有效',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID(补算时)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date_reason` (`user_id`, `record_date`, `reason`),
    KEY `idx_record_date` (`record_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='里程流水表';

-- ========================================
-- 4. 路线节点表 t_route_node
-- ========================================
CREATE TABLE `t_route_node` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `node_code` VARCHAR(32) NOT NULL COMMENT '节点编码',
    `node_name` VARCHAR(64) NOT NULL COMMENT '节点名称',
    `mileage_threshold` DECIMAL(10,2) NOT NULL COMMENT '解锁所需里程(公里)',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度(地图展示用)',
    `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度(地图展示用)',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '节点简介',
    `icon_url` VARCHAR(512) DEFAULT NULL COMMENT '节点图标URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_code` (`node_code`),
    KEY `idx_mileage_threshold` (`mileage_threshold`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路线节点表';

-- ========================================
-- 5. 节点内容表 t_node_content
-- ========================================
CREATE TABLE `t_node_content` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `node_id` BIGINT UNSIGNED NOT NULL COMMENT '节点ID',
    `version` INT NOT NULL DEFAULT 1 COMMENT '内容版本号',
    `title` VARCHAR(128) NOT NULL COMMENT '内容标题',
    `video_url` VARCHAR(512) DEFAULT NULL COMMENT '视频URL',
    `video_cover_url` VARCHAR(512) DEFAULT NULL COMMENT '视频封面URL',
    `video_duration` INT DEFAULT NULL COMMENT '视频时长(秒)',
    `content_text` TEXT COMMENT '文字内容(富文本)',
    `content_summary` VARCHAR(512) DEFAULT NULL COMMENT '内容摘要',
    `is_current` TINYINT NOT NULL DEFAULT 1 COMMENT '是否当前版本: 0-历史 1-当前',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `created_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_node_id` (`node_id`),
    KEY `idx_is_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点内容表';

-- ========================================
-- 6. 用户节点进度表 t_user_node_progress
-- ========================================
CREATE TABLE `t_user_node_progress` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `node_id` BIGINT UNSIGNED NOT NULL COMMENT '节点ID',
    `unlock_status` TINYINT NOT NULL DEFAULT 0 COMMENT '解锁状态: 0-未解锁 1-已解锁',
    `unlocked_at` DATETIME DEFAULT NULL COMMENT '首次解锁时间',
    `unlocked_mileage` DECIMAL(10,2) DEFAULT NULL COMMENT '解锁时的累计里程',
    `view_status` TINYINT NOT NULL DEFAULT 0 COMMENT '观看状态: 0-未看 1-已看',
    `first_view_at` DATETIME DEFAULT NULL COMMENT '首次观看时间',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '观看次数',
    `last_view_at` DATETIME DEFAULT NULL COMMENT '最后观看时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_node` (`user_id`, `node_id`),
    KEY `idx_node_id` (`node_id`),
    KEY `idx_unlock_status` (`unlock_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户节点进度表';

-- ========================================
-- 7. 成就配置表 t_achievement
-- ========================================
CREATE TABLE `t_achievement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `achievement_code` VARCHAR(32) NOT NULL COMMENT '成就编码',
    `achievement_name` VARCHAR(64) NOT NULL COMMENT '成就名称',
    `description` VARCHAR(256) NOT NULL COMMENT '成就描述',
    `icon_url` VARCHAR(512) DEFAULT NULL COMMENT '成就图标URL',
    `icon_locked_url` VARCHAR(512) DEFAULT NULL COMMENT '未解锁图标URL',
    `achievement_type` VARCHAR(32) NOT NULL COMMENT '成就类型: MILEAGE/CONTINUOUS/NODE',
    `condition_type` VARCHAR(32) NOT NULL COMMENT '条件类型: GTE/EQ/COUNT',
    `condition_value` INT NOT NULL COMMENT '条件值',
    `points` INT NOT NULL DEFAULT 0 COMMENT '成就积分',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_achievement_code` (`achievement_code`),
    KEY `idx_achievement_type` (`achievement_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就配置表';

-- ========================================
-- 8. 用户成就表 t_user_achievement
-- ========================================
CREATE TABLE `t_user_achievement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `achievement_id` BIGINT UNSIGNED NOT NULL COMMENT '成就ID',
    `unlocked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    `unlocked_value` INT DEFAULT NULL COMMENT '达成时的数值',
    `is_displayed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已展示: 0-未展示 1-已展示',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`),
    KEY `idx_achievement_id` (`achievement_id`),
    KEY `idx_unlocked_at` (`unlocked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';

-- ========================================
-- 9. 排行榜快照表 t_leaderboard_snapshot
-- ========================================
CREATE TABLE `t_leaderboard_snapshot` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `snapshot_date` DATE NOT NULL COMMENT '快照日期',
    `board_type` VARCHAR(32) NOT NULL COMMENT '榜单类型: TOTAL/WEEK/MONTH/CLASS/GRADE',
    `board_key` VARCHAR(64) DEFAULT NULL COMMENT '榜单键(班级ID/年级等)',
    `rank_no` INT NOT NULL COMMENT '排名',
    `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID(个人榜)',
    `group_id` VARCHAR(64) DEFAULT NULL COMMENT '分组ID(班级/年级榜)',
    `group_name` VARCHAR(64) DEFAULT NULL COMMENT '分组名称',
    `mileage` DECIMAL(10,2) NOT NULL COMMENT '里程数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_snapshot_date_type` (`snapshot_date`, `board_type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_rank_no` (`rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜快照表';

-- ========================================
-- 10. 系统配置表 t_system_config
-- ========================================
CREATE TABLE `t_system_config` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(64) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
    `config_type` VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '值类型: STRING/INT/DECIMAL/JSON',
    `config_group` VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' COMMENT '配置分组',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '配置说明',
    `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开: 0-仅后台 1-客户端可见',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ========================================
-- 11. 管理员表 t_admin (独立于学生用户)
-- ========================================
CREATE TABLE `t_admin` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `role` VARCHAR(32) NOT NULL DEFAULT 'ADMIN' COMMENT '角色: ADMIN/SUPER_ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ========================================
-- 12. 学生信息底表 t_student_info (导入的学生数据，用于认证)
-- ========================================
CREATE TABLE `t_student_info` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_no` VARCHAR(64) NOT NULL COMMENT '学号',
    `name` VARCHAR(64) NOT NULL COMMENT '姓名',
    `gender` VARCHAR(4) DEFAULT NULL COMMENT '性别',
    `major` VARCHAR(64) DEFAULT NULL COMMENT '专业',
    `class_name` VARCHAR(64) DEFAULT NULL COMMENT '班级',
    `grade` VARCHAR(16) DEFAULT NULL COMMENT '年级(如2022级)',
    `enroll_year` INT DEFAULT NULL COMMENT '入学年份',
    `college` VARCHAR(64) DEFAULT '智能制造与信息工程学院' COMMENT '学院',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `is_bound` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已绑定: 0-未绑定 1-已绑定',
    `bound_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '绑定的用户ID',
    `bound_at` DATETIME DEFAULT NULL COMMENT '绑定时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `import_batch` VARCHAR(64) DEFAULT NULL COMMENT '导入批次号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_class_name` (`class_name`),
    KEY `idx_major` (`major`),
    KEY `idx_grade` (`grade`),
    KEY `idx_is_bound` (`is_bound`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息底表';
