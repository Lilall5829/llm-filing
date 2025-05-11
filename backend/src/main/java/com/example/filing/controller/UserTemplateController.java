package com.example.filing.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.filing.dto.request.ApplyTemplateRequest;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.service.UserTemplateService;
import com.example.filing.util.Result;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/userTemplate")
@RequiredArgsConstructor
public class UserTemplateController {

    private final UserTemplateRepository userTemplateRepository;
    private final UserTemplateService userTemplateService;

    /**
     * 分页获取用户模板列表
     *
     * @param userId 用户ID
     * @param page   页码（从0开始）
     * @param size   每页大小
     * @return 用户模板分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserTemplate>> getUserTemplateList(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<UserTemplate> userTemplates = userTemplateRepository.findByUserId(userId, pageRequest);

        return ResponseEntity.ok(userTemplates);
    }

    /**
     * 用户申请模板或管理员发送模板
     *
     * @param templateId 模板ID
     * @param request    请求体
     * @param auth       认证信息
     * @return 创建的用户模板关系ID列表
     */
    @PostMapping("/applyTemplate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<List<String>>> applyTemplate(
            @RequestParam String templateId,
            @RequestBody ApplyTemplateRequest request,
            Authentication auth) {

        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.failed("用户ID列表不能为空"));
        }

        // 从Authentication获取用户ID和角色
        String userId = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 调用服务层方法
        Result<List<String>> result = userTemplateService.applyTemplate(
                templateId, request.getUserIds(), userId, isAdmin);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 更新模板状态
     *
     * @param id      用户模板关系ID
     * @param status  新状态
     * @param remarks 备注信息
     * @param auth    认证信息
     * @return 更新结果
     */
    @PostMapping("/updateTemplateStatus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<String>> updateTemplateStatus(
            @RequestParam String id,
            @RequestParam Integer status,
            @RequestBody(required = false) String remarks,
            Authentication auth) {

        // 从Authentication获取用户ID和角色
        String userId = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 调用服务层方法
        Result<String> result = userTemplateService.updateTemplateStatus(id, status, remarks, userId, isAdmin);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取模板内容
     *
     * @param id 用户模板关系ID
     * @return 模板内容
     */
    @GetMapping("/getTemplateContent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<String>> getTemplateContent(@RequestParam String id) {
        Result<String> result = userTemplateService.getTemplateContent(id);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 保存模板内容
     *
     * @param id      用户模板关系ID
     * @param content 内容JSON
     * @param auth    认证信息
     * @return 保存结果
     */
    @PostMapping("/saveTemplateContent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<String>> saveTemplateContent(
            @RequestParam String id,
            @RequestBody String content,
            Authentication auth) {

        // 从Authentication获取用户ID
        String userId = auth.getName();

        // 调用服务层方法
        Result<String> result = userTemplateService.saveTemplateContent(id, content, userId);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}