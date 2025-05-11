-- 清空测试相关的表
DELETE FROM user_template;
DELETE FROM template_registry;
DELETE FROM sys_user WHERE login_name IN ('testuser', 'anotheruser'); 