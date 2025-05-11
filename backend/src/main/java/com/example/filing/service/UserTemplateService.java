package com.example.filing.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.example.filing.entity.AuditLog;
import com.example.filing.entity.UserTemplate;
import com.example.filing.util.Result;

/**
 * 用户模板关系服务接口
 */
public interface UserTemplateService {

    /**
     * 分页查询用户模板列表
     * 
     * @param userId   用户ID
     * @param current  当前页码
     * @param pageSize 每页大小
     * @return 用户模板分页数据
     */
    Result<Page<UserTemplate>> getUserTemplateList(String userId, Integer current, Integer pageSize);

    /**
     * 用户申请模板或管理员发送模板
     * 
     * @param templateId 模板ID
     * @param userIds    用户ID列表
     * @param operatorId 操作人ID
     * @param isAdmin    是否是管理员操作
     * @return 创建的用户模板关系ID列表
     */
    Result<List<String>> applyTemplate(String templateId, List<String> userIds, String operatorId, boolean isAdmin);

    /**
     * 更新模板状态
     * 
     * @param id      用户模板关系ID
     * @param status  新状态值
     * @param remarks 备注信息
     * @param userId  操作用户ID
     * @param isAdmin 是否是管理员
     * @return 更新结果
     */
    Result<String> updateTemplateStatus(String id, Integer status, String remarks, String userId, boolean isAdmin);

    /**
     * 获取模板内容
     * 
     * @param id 用户模板关系ID
     * @return 模板内容
     */
    Result<String> getTemplateContent(String id);

    /**
     * 保存模板内容
     * 
     * @param id      用户模板关系ID
     * @param content 内容JSON
     * @param userId  用户ID
     * @return 保存结果
     */
    Result<String> saveTemplateContent(String id, String content, String userId);

    /**
     * 获取模板审核历史
     *
     * @param id 用户模板关系ID
     * @return 审核历史列表
     */
    Result<List<AuditLog>> getTemplateAuditHistory(String id);
}