package com.example.filing.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.filing.entity.AuditLog;

/**
 * 审核日志仓库接口
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /**
     * 根据实体类型和ID查找审核日志
     * 
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @param pageable   分页信息
     * @return 分页的审核日志
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    /**
     * 根据实体类型和ID查找审核日志
     * 
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @return 审核日志列表
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByOperationTimeDesc(String entityType, String entityId);

    /**
     * 根据实体类型、ID和操作类型查找审核日志
     * 
     * @param entityType    实体类型
     * @param entityId      实体ID
     * @param operationType 操作类型
     * @return 审核日志列表
     */
    List<AuditLog> findByEntityTypeAndEntityIdAndOperationType(String entityType, String entityId,
            String operationType);

    /**
     * 根据操作人ID查找审核日志
     * 
     * @param operatorId 操作人ID
     * @param pageable   分页信息
     * @return 分页的审核日志
     */
    Page<AuditLog> findByOperatorId(String operatorId, Pageable pageable);
}