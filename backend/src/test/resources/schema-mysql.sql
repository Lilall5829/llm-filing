-- 创建sys_user表
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
    `id` VARCHAR(36) PRIMARY KEY,
    `login_name` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `user_name` VARCHAR(50) NOT NULL,
    `role` INT NOT NULL COMMENT '角色：1-管理员，2-普通用户',
    `status` INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `last_login_time` DATETIME,
    UNIQUE KEY `uk_login_name` (`login_name`)
);

-- 创建模板表
DROP TABLE IF EXISTS `template_registry`;

CREATE TABLE `template_registry` (
    `id` VARCHAR(36) PRIMARY KEY,
    `template_code` VARCHAR(50) COMMENT '模板编号',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_description` TEXT COMMENT '模板描述',
    `template_type` VARCHAR(50) COMMENT '模板类型',
    `template_content` LONGTEXT COMMENT '模板内容（JSON格式）',
    `file_path` VARCHAR(255) COMMENT '文件路径',
    `create_by` VARCHAR(36) COMMENT '创建人ID',
    `update_by` VARCHAR(36) COMMENT '更新人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
);

-- 创建用户模板关系表
DROP TABLE IF EXISTS `user_template`;

CREATE TABLE `user_template` (
    `id` VARCHAR(36) PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `template_id` VARCHAR(36) NOT NULL COMMENT '模板ID',
    `content` LONGTEXT COMMENT '填写内容（JSON格式）',
    `status` INT DEFAULT 0 COMMENT '状态：0-待审核，1-申请通过，2-拒绝申请，3-待填写，4-填写中，5-审核中，6-审核通过，7-退回',
    `remarks` TEXT COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_status` (`status`)
);

-- 创建操作日志表
DROP TABLE IF EXISTS `operation_log`;

CREATE TABLE `operation_log` (
    `id` VARCHAR(36) PRIMARY KEY,
    `user_id` VARCHAR(36) COMMENT '操作用户ID',
    `operation_type` VARCHAR(50) COMMENT '操作类型',
    `operation_content` TEXT COMMENT '操作内容',
    `entity_type` VARCHAR(50) COMMENT '实体类型',
    `entity_id` VARCHAR(36) COMMENT '实体ID',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `operation_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 创建审计日志表
DROP TABLE IF EXISTS `audit_log`;

CREATE TABLE `audit_log` (
    `id` VARCHAR(36) PRIMARY KEY,
    `operator_id` VARCHAR(36) COMMENT '操作用户ID',
    `operator_name` VARCHAR(50) COMMENT '操作用户名称',
    `operation_type` VARCHAR(50) COMMENT '操作类型',
    `details` TEXT COMMENT '详细内容',
    `entity_type` VARCHAR(50) COMMENT '实体类型',
    `entity_id` VARCHAR(36) COMMENT '实体ID',
    `old_status` INT COMMENT '旧状态',
    `new_status` INT COMMENT '新状态',
    `is_admin` BOOLEAN COMMENT '是否是管理员操作',
    `operation_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
); 