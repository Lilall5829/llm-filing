-- 清理已有数据
DELETE FROM `user_template`;
DELETE FROM `template_registry`;
DELETE FROM `sys_user`;

-- 插入管理员用户数据（密码: admin123）
-- 注意：这个BCrypt哈希值必须与测试使用的密码匹配
INSERT INTO `sys_user` (`id`, `login_name`, `password`, `user_name`, `role`, `status`, `create_time`, `update_time`)
VALUES 
('admin-id', 'admin', '$2a$10$rDkPvvAFV6MwGDo7jSMTP.xWBrIxH0A3iQ3IJJUszpgX5EPk/g.Tm', '管理员', 1, 1, NOW(), NOW());

-- 插入普通用户数据（密码: password）
-- 注意：这个BCrypt哈希值必须与测试使用的密码匹配
INSERT INTO `sys_user` (`id`, `login_name`, `password`, `user_name`, `role`, `status`, `create_time`, `update_time`)
VALUES 
('user-id', 'testuser', '$2a$10$rDkPvvAFV6MwGDo7jSMTP.xWBrIxH0A3iQ3IJJUszpgX5EPk/g.Tm', '测试用户', 2, 1, NOW(), NOW());

-- 插入测试模板
INSERT INTO `template_registry` (`id`, `template_code`, `template_name`, `template_description`, `template_type`, `template_content`, `create_by`, `create_time`, `update_time`)
VALUES 
('template-id', 'TPL001', '测试模板', '用于测试的模板', '测试', '{"formFields":[{"name":"field1","label":"测试字段","type":"text","required":true}]}', 'admin-id', NOW(), NOW());

-- 插入测试用户模板关系（状态3-待填写）
INSERT INTO `user_template` (`id`, `user_id`, `template_id`, `status`, `create_time`, `update_time`)
VALUES 
('user-template-id', 'user-id', 'template-id', 3, NOW(), NOW()); 