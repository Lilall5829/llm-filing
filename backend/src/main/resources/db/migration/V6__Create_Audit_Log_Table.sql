-- 创建审核日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id VARCHAR(36) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id VARCHAR(36) NOT NULL COMMENT '实体ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    old_status INT COMMENT '操作前状态',
    new_status INT COMMENT '操作后状态',
    details TEXT COMMENT '详细信息',
    operator_id VARCHAR(36) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人名称',
    is_admin BOOLEAN NOT NULL COMMENT '是否是管理员操作',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_entity (entity_type, entity_id),
    KEY idx_operator (operator_id),
    KEY idx_operation_time (operation_time)
); 