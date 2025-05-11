-- 清理已有数据
DELETE FROM `sys_user` WHERE `login_name` = 'admin';

-- 插入管理员用户数据（密码: admin123）
-- 注意：这个BCrypt哈希值已经被确认与当前测试环境中的passwordEncoder兼容
INSERT INTO `sys_user` (`id`, `login_name`, `password`, `user_name`, `role`, `status`, `create_time`, `update_time`)
VALUES 
('admin-test-id', 'admin', '$2a$10$J8YdygzU0NRggY6lSpXtyeJaJ8jDN5QVrGXS53UjC7vJITSm3imJC', 'Admin User', 1, 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()); 