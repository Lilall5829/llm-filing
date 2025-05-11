-- Add default admin user with encrypted password 'admin123'
INSERT IGNORE INTO sys_user (id, login_name, password, user_name, role, status, create_time, update_time)
VALUES (
    UUID(),
    'admin',
    '$2a$10$rDMgsmMpQxK3hQyuJUZCe.CWAhQjNJR8pKm/TLSLMgkE4jQJJf93K',
    'Administrator',
    1, -- Role: 1-Admin
    1, -- Status: 1-Enabled
    NOW(),
    NOW()
);

-- Add default regular user with encrypted password 'user123'
INSERT IGNORE INTO sys_user (id, login_name, password, user_name, role, status, create_time, update_time)
VALUES (
    UUID(),
    'user',
    '$2a$10$TJ9ogBb6bqvW17fpfV0QZeTSQs1fBnS.hZj5XerH48yAFGCn/zJSa',
    'Regular User',
    2, -- Role: 2-Regular user
    1, -- Status: 1-Enabled
    NOW(),
    NOW()
); 