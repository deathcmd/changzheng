USE `changzheng_db`;

-- 微信用户在完成学生认证之前没有学号。
ALTER TABLE `t_user`
    MODIFY COLUMN `student_no` VARCHAR(64) NULL COMMENT '学号(AES加密存储，认证后写入)',
    MODIFY COLUMN `student_no_suffix` VARCHAR(4) NULL COMMENT '学号后4位(用于展示)';

-- Disable the historical well-known bootstrap account when it still uses the
-- published password hash. Configure ADMIN_USERNAME and ADMIN_PASSWORD to
-- create a replacement account on the next application start.
UPDATE `t_admin`
SET `status` = 0
WHERE `username` = 'admin'
  AND `password` = '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW';
