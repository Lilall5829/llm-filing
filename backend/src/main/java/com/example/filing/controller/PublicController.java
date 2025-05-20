package com.example.filing.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.filing.entity.TemplateRegistry;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.util.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共接口控制器
 * 提供给普通用户和管理员共同使用的API
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final TemplateRegistryRepository templateRegistryRepository;

    /**
     * 获取公开模板列表
     * 允许普通用户访问的模板列表接口
     *
     * @param templateName 模板名称（可选，筛选条件）
     * @param templateCode 模板编号（可选，筛选条件）
     * @param templateType 模板类型（可选，筛选条件）
     * @param pageNum      页码（从1开始）
     * @param pageSize     每页大小
     * @return 模板分页列表
     */
    @GetMapping("/templates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Page<TemplateRegistry>>> getPublicTemplates(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        try {
            log.info("获取公开模板列表: templateName={}, templateCode={}, templateType={}, pageNum={}, pageSize={}",
                    templateName, templateCode, templateType, pageNum, pageSize);

            // 创建分页请求
            PageRequest pageRequest = PageRequest.of(
                    pageNum - 1, // 前端从1开始，后端从0开始
                    pageSize,
                    Sort.by("updateTime").descending());

            // 查询模板列表
            Page<TemplateRegistry> templates = templateRegistryRepository.findTemplates(
                    templateCode,
                    templateName,
                    templateType,
                    pageRequest);

            log.info("成功获取公开模板列表，数量: {}", templates.getTotalElements());

            // 返回成功结果
            return ResponseEntity.ok(Result.success(templates));
        } catch (Exception e) {
            log.error("获取公开模板列表失败", e);
            return ResponseEntity.badRequest().body(Result.failed("获取模板列表失败: " + e.getMessage()));
        }
    }
}