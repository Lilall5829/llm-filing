package com.example.filing.dto.request;

import lombok.Data;

/**
 * 用户模板状态更新请求
 */
@Data
public class UserTemplateStatusRequest {

    /**
     * 用户模板ID
     */
    private String id;

    /**
     * 状态值
     * 0-待审核，1-申请通过，2-拒绝申请，3-待填写，4-填写中，5-审核中，6-审核通过，7-退回
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remarks;
}