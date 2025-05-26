package com.example.filing.dto.response;

import java.time.LocalDateTime;

import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;

import lombok.Data;

/**
 * 用户模板关系DTO，包含模板的基本信息
 */
@Data
public class UserTemplateDTO {
    private String id;
    private String userId;
    private String templateId;
    private String content;
    private Integer status;
    private String remarks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 模板基本信息
    private String templateCode;
    private String templateName;
    private String templateDescription;
    private String templateType;

    // 用户基本信息
    private String userName;

    // 状态描述
    private String statusDesc;

    /**
     * 从UserTemplate实体转换为DTO
     * 
     * @param userTemplate     UserTemplate实体
     * @param templateRegistry TemplateRegistry实体（可选）
     * @return UserTemplateDTO
     */
    public static UserTemplateDTO fromEntity(UserTemplate userTemplate, TemplateRegistry templateRegistry) {
        UserTemplateDTO dto = new UserTemplateDTO();

        // 复制UserTemplate字段
        dto.setId(userTemplate.getId());
        dto.setUserId(userTemplate.getUserId());
        dto.setTemplateId(userTemplate.getTemplateId());
        dto.setContent(userTemplate.getContent());
        dto.setStatus(userTemplate.getStatus());
        dto.setRemarks(userTemplate.getRemarks());
        dto.setCreateTime(userTemplate.getCreateTime());
        dto.setUpdateTime(userTemplate.getUpdateTime());

        // 设置状态描述
        dto.setStatusDesc(getStatusDescription(userTemplate.getStatus()));

        // 如果提供了模板实体，则复制模板信息
        if (templateRegistry != null) {
            dto.setTemplateCode(templateRegistry.getTemplateCode());
            dto.setTemplateName(templateRegistry.getTemplateName());
            dto.setTemplateDescription(templateRegistry.getTemplateDescription());
            dto.setTemplateType(templateRegistry.getTemplateType());
        }

        return dto;
    }

    /**
     * 从UserTemplate实体转换为DTO，包含用户和模板信息
     * 
     * @param userTemplate     UserTemplate实体
     * @param templateRegistry TemplateRegistry实体（可选）
     * @param sysUser          SysUser实体（可选）
     * @return UserTemplateDTO
     */
    public static UserTemplateDTO fromEntity(UserTemplate userTemplate, TemplateRegistry templateRegistry,
            SysUser sysUser) {
        UserTemplateDTO dto = fromEntity(userTemplate, templateRegistry);

        // 如果提供了用户实体，则复制用户信息
        if (sysUser != null) {
            dto.setUserName(sysUser.getUserName());
        }

        return dto;
    }

    /**
     * 获取状态描述
     * 
     * @param status 状态编码
     * @return 状态描述
     */
    private static String getStatusDescription(Integer status) {
        if (status == null)
            return "未知";

        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "申请通过";
            case 2:
                return "拒绝申请";
            case 3:
                return "待填写";
            case 4:
                return "填写中";
            case 5:
                return "审核中";
            case 6:
                return "审核通过";
            case 7:
                return "退回";
            default:
                return "未知";
        }
    }
}