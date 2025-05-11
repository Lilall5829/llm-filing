package com.example.filing.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 审核日志实体类
 */
@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    /**
     * 实体类型（如user_template）
     */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    /**
     * 实体ID
     */
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    /**
     * 操作类型（如状态变更、内容修改）
     */
    @Column(name = "operation_type", nullable = false)
    private String operationType;

    /**
     * 操作前状态
     */
    @Column(name = "old_status")
    private Integer oldStatus;

    /**
     * 操作后状态
     */
    @Column(name = "new_status")
    private Integer newStatus;

    /**
     * 详细信息（如备注）
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * 操作人ID
     */
    @Column(name = "operator_id", nullable = false)
    private String operatorId;

    /**
     * 操作人名称
     */
    @Column(name = "operator_name")
    private String operatorName;

    /**
     * 是否是管理员操作
     */
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime = LocalDateTime.now();
}