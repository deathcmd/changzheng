USE `changzheng_db`;

-- Keep college and major as separate concepts. Existing records remain valid
-- and will receive their major on the next successful student binding/import.
ALTER TABLE `t_user`
    ADD COLUMN `major` VARCHAR(64) DEFAULT NULL COMMENT '专业' AFTER `college`;

UPDATE `t_user` u
INNER JOIN `t_student_info` s ON s.bound_user_id = u.id AND s.is_bound = 1
SET u.major = s.major
WHERE u.major IS NULL;

-- A ledger is an append-only audit trail. Repeated synchronisations for the
-- same day therefore create additional delta rows instead of mutating history.
ALTER TABLE `t_mileage_ledger`
    DROP INDEX `uk_user_date_reason`,
    ADD KEY `idx_user_date_reason` (`user_id`, `record_date`, `reason`);

-- Normalise node content fields used by both the admin application and the
-- mini program while retaining the legacy columns for backwards compatibility.
ALTER TABLE `t_node_content`
    ADD COLUMN `content_type` VARCHAR(16) NOT NULL DEFAULT 'video' COMMENT '内容类型: video/audio/article/image' AFTER `title`,
    ADD COLUMN `media_url` VARCHAR(512) DEFAULT NULL COMMENT '媒体地址' AFTER `content_type`,
    ADD COLUMN `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '封面地址' AFTER `media_url`,
    ADD COLUMN `duration_label` VARCHAR(32) DEFAULT NULL COMMENT '时长或字数说明' AFTER `cover_url`,
    ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号' AFTER `duration_label`,
    ADD COLUMN `auto_play` TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动播放' AFTER `sort_order`,
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用' AFTER `auto_play`,
    ADD KEY `idx_node_status_sort` (`node_id`, `status`, `sort_order`);

UPDATE `t_node_content`
SET `content_type` = CASE
        WHEN `video_url` IS NOT NULL AND `video_url` <> '' THEN 'video'
        ELSE 'article'
    END,
    `media_url` = COALESCE(`media_url`, `video_url`),
    `cover_url` = COALESCE(`cover_url`, `video_cover_url`),
    `duration_label` = COALESCE(`duration_label`,
        CASE WHEN `video_duration` IS NULL THEN NULL ELSE CONCAT(`video_duration`, '秒') END),
    `sort_order` = `version`,
    `status` = `is_current`;

-- Clients render articles as plain text. Convert legacy HTML paragraph and
-- line-break markup so old seed data remains readable without an HTML sink.
UPDATE `t_node_content`
SET `content_text` = TRIM(
        REGEXP_REPLACE(
            REGEXP_REPLACE(
                REGEXP_REPLACE(`content_text`, '<[[:space:]]*br[[:space:]]*/?[[:space:]]*>', CHAR(10), 1, 0, 'i'),
                '</[[:space:]]*p[[:space:]]*>', CONCAT(CHAR(10), CHAR(10)), 1, 0, 'i'),
            '<[^>]+>', '', 1, 0, 'i'))
WHERE `content_text` IS NOT NULL;

CREATE TABLE `t_user_learn_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `content_id` BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
    `learned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近学习时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_content` (`user_id`, `content_id`),
    KEY `idx_content_id` (`content_id`),
    KEY `idx_learned_at` (`learned_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户学习记录表';
