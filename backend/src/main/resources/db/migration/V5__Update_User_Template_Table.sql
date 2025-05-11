CREATE TABLE IF NOT EXISTS user_template (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    template_id VARCHAR(36) NOT NULL COMMENT '模板ID',
    content LONGTEXT COMMENT '填写内容（JSON格式）',
    status INT DEFAULT 0 COMMENT '状态：0-待审核，1-申请通过，2-拒绝申请，3-待填写，4-填写中，5-审核中，6-审核通过，7-退回',
    remarks TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_template_id (template_id),
    KEY idx_status (status)
); 