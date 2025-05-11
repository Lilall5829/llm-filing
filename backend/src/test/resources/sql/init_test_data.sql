-- 初始化一些测试模板类型
INSERT INTO template_type (id, type_name, type_code, create_time, update_time) 
VALUES (UUID(), 'TEST', 'TEST', NOW(), NOW())
ON DUPLICATE KEY UPDATE type_name = 'TEST'; 