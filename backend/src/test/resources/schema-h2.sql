-- 设置H2数据库为MySQL兼容模式
SET MODE MySQL;

-- 创建sys_user表
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id VARCHAR(36) PRIMARY KEY,
    login_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    role INT NOT NULL,
    status INT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    last_login_time TIMESTAMP,
    CONSTRAINT uk_login_name UNIQUE (login_name)
);

-- 创建模板表
DROP TABLE IF EXISTS template_registry;

CREATE TABLE template_registry (
    id VARCHAR(36) PRIMARY KEY,
    template_code VARCHAR(50),
    template_name VARCHAR(100) NOT NULL,
    template_description CLOB,
    template_type VARCHAR(50),
    template_content CLOB,
    file_path VARCHAR(255),
    create_by VARCHAR(36),
    update_by VARCHAR(36),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 创建用户模板关系表
DROP TABLE IF EXISTS user_template;

CREATE TABLE user_template (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    content CLOB,
    status INT DEFAULT 0,
    remarks CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加索引
CREATE INDEX idx_user_id ON user_template(user_id);
CREATE INDEX idx_template_id ON user_template(template_id);
CREATE INDEX idx_status ON user_template(status);

-- 创建Flyway历史表
DROP TABLE IF EXISTS flyway_schema_history;

CREATE TABLE flyway_schema_history (
    installed_rank INT NOT NULL PRIMARY KEY,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL
); 