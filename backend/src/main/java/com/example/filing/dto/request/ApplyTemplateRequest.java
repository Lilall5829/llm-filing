package com.example.filing.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申请模板DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyTemplateRequest {

    /**
     * 用户ID列表
     */
    private List<String> userIds;
}