package com.example.filing.service;

import com.example.filing.dto.request.TemplateRegistryRequest;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.util.Result;

/**
 * 模板服务接口
 */
public interface TemplateRegistryService {

    /**
     * 分页查询模板列表
     * 
     * @param templateCode 模板编号
     * @param templateName 模板名称
     * @param templateType 模板类型
     * @param current      当前页码
     * @param pageSize     每页大小
     * @return 模板分页数据结果
     */
    Result<?> findTemplatesByPage(String templateCode, String templateName, String templateType,
            Integer current, Integer pageSize);

    /**
     * 创建或更新模板
     * 
     * @param request 模板请求
     * @param userId  操作用户ID
     * @return 创建/更新结果
     */
    Result<TemplateRegistry> saveTemplateRegistry(TemplateRegistryRequest request, String userId);

    /**
     * 根据ID获取模板详情
     * 
     * @param id 模板ID
     * @return 模板详情
     */
    Result<TemplateRegistry> getTemplateRegistryById(String id);

    /**
     * 删除模板
     * 
     * @param id 模板ID
     * @return 删除结果
     */
    Result<String> deleteTemplate(String id);

    /**
     * 更新模板文件路径
     * 
     * @param id       模板ID
     * @param filePath 文件路径
     * @return 更新结果
     */
    Result<String> updateTemplateFilePath(String id, String filePath);

    /**
     * 更新模板内容
     * 
     * @param id              模板ID
     * @param templateContent 模板内容（JSON格式）
     * @return 更新结果
     */
    Result<String> updateTemplateContent(String id, String templateContent);
}