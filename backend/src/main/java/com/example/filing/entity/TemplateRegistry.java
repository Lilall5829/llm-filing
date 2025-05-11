package com.example.filing.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 模板实体
 */
@Data
@Entity
@Table(name = "template_registry")
@Where(clause = "deleted = 0")
public class TemplateRegistry {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    /**
     * 模板编号
     */
    @Column(name = "template_code")
    private String templateCode;

    /**
     * 模板名称
     */
    @Column(name = "template_name", nullable = false)
    private String templateName;

    /**
     * 模板描述
     */
    @Column(name = "template_description", columnDefinition = "TEXT")
    private String templateDescription;

    /**
     * 模板类型
     */
    @Column(name = "template_type")
    private String templateType;

    /**
     * 模板内容（JSON格式）
     */
    @Column(name = "template_content", columnDefinition = "LONGTEXT")
    private String templateContent;

    /**
     * 文件路径
     */
    @Column(name = "file_path")
    private String filePath;

    /**
     * 创建人ID
     */
    @Column(name = "create_by")
    private String createBy;

    /**
     * 更新人ID
     */
    @Column(name = "update_by")
    private String updateBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updateTime;

    /**
     * 删除标志：0-未删除，1-已删除
     */
    @Column(name = "deleted", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer deleted = 0;
}