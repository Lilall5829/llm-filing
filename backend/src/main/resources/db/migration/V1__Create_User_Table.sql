CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(36) PRIMARY KEY,
    login_name VARCHAR(50) NOT NULL COMMENT 'Login name',
    password VARCHAR(255) NOT NULL COMMENT 'Password',
    user_name VARCHAR(50) NOT NULL COMMENT 'User name',
    role INT NOT NULL COMMENT 'Role: 1-Admin, 2-Regular user',
    status INT DEFAULT 1 COMMENT 'Status: 0-Disabled, 1-Enabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time DATETIME COMMENT 'Last login time',
    UNIQUE KEY uk_login_name (login_name)
); 