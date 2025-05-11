-- 创建模板表
CREATE TABLE IF NOT EXISTS template_registry (
    id VARCHAR(36) PRIMARY KEY,
    template_code VARCHAR(50) COMMENT '模板编号',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_description TEXT COMMENT '模板描述',
    template_type VARCHAR(50) COMMENT '模板类型',
    template_content LONGTEXT COMMENT '模板内容（JSON格式）',
    file_path VARCHAR(255) COMMENT '文件路径',
    create_by VARCHAR(36) COMMENT '创建人ID',
    update_by VARCHAR(36) COMMENT '更新人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
); 