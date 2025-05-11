package com.example.filing.dto.request;

import lombok.Data;

/**
 * 模板创建/更新请求DTO
 */
@Data
public class TemplateRegistryRequest {

    /**
     * 模板编号
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String templateDescription;

    /**
     * 模板类型
     */
    private String templateType;

    /**
     * 模板内容（JSON格式）
     */
    private String templateContent;

    /**
     * 文件路径
     */
    private String filePath;
}