package com.example.filing.controller;

import java.util.List;
import java.util.Optional;

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

import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.dto.request.ApplyTemplateRequest;
import com.example.filing.entity.AuditLog;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.service.UserTemplateService;
import com.example.filing.util.Result;

import lombok.RequiredArgsConstructor;

/**
 * 用户模板关系控制器
 */
@RestController
@RequestMapping("/api/userTemplate")
@RequiredArgsConstructor
public class UserTemplateController {

    private final UserTemplateService userTemplateService;
    private final UserTemplateRepository userTemplateRepository;

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
     * 管理员审核用户提交的模板
     *
     * @param id      用户模板关系ID
     * @param status  新状态值(6-审核通过,7-退回)
     * @param remarks 审核意见/备注信息
     * @param auth    认证信息
     * @return 审核结果
     */
    @PostMapping("/reviewTemplate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<String>> reviewTemplate(
            @RequestParam String id,
            @RequestParam Integer status,
            @RequestBody(required = false) String remarks,
            Authentication auth) {

        // 从Authentication获取管理员ID
        String adminId = auth.getName();

        // 验证状态值是否有效
        if (status != UserTemplateStatus.REVIEW_APPROVED && status != UserTemplateStatus.RETURNED) {
            return ResponseEntity.badRequest().body(Result.failed("无效的审核状态，只能是审核通过(6)或退回(7)"));
        }

        // 调用服务层方法
        Result<String> result = userTemplateService.updateTemplateStatus(id, status, remarks, adminId, true);

        if (result.getCode() == 200) {
            // 审核成功后，根据状态返回相应的成功信息
            String message = status == UserTemplateStatus.REVIEW_APPROVED ? "审核通过" : "已退回用户修改";
            return ResponseEntity.ok(Result.success(message));
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 用户提交表单内容进行审核
     *
     * @param id   用户模板关系ID
     * @param auth 认证信息
     * @return 提交结果
     */
    @PostMapping("/submitForReview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<String>> submitForReview(
            @RequestParam String id,
            Authentication auth) {

        // 从Authentication获取用户ID
        String userId = auth.getName();

        // 验证模板是否存在且状态是否允许提交
        Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
        if (!userTemplateOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Result.failed("模板不存在"));
        }

        UserTemplate userTemplate = userTemplateOpt.get();

        // 验证是否是用户自己的模板
        if (!userTemplate.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.failed("无权操作他人的模板"));
        }

        // 验证模板状态是否允许提交审核（必须是填写中(4)或退回(7)状态）
        int currentStatus = userTemplate.getStatus();
        if (currentStatus != UserTemplateStatus.FILLING && currentStatus != UserTemplateStatus.RETURNED) {
            String statusDesc = currentStatus == UserTemplateStatus.UNDER_REVIEW ? "审核中"
                    : currentStatus == UserTemplateStatus.REVIEW_APPROVED ? "已审核通过" : "当前状态";
            return ResponseEntity.badRequest().body(Result.failed("模板已是" + statusDesc + "，不能提交审核"));
        }

        // 验证模板是否有内容
        if (userTemplate.getContent() == null || userTemplate.getContent().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.failed("模板内容不能为空，请先填写内容"));
        }

        // 调用服务层方法，将状态设为审核中(5)
        Result<String> result = userTemplateService.updateTemplateStatus(
                id, UserTemplateStatus.UNDER_REVIEW, "用户提交审核", userId, false);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(Result.success("已提交审核，请等待管理员审核"));
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

        String userId = auth.getName();
        Result<String> result = userTemplateService.saveTemplateContent(id, content, userId);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取模板审核历史
     *
     * @param id 用户模板关系ID
     * @return 审核历史列表
     */
    @GetMapping("/getAuditHistory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<List<AuditLog>>> getAuditHistory(@RequestParam String id, Authentication auth) {
        // 验证操作权限（管理员可以查看任何模板的历史，普通用户只能查看自己的）
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // 如果不是管理员，验证是否是自己的模板
            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent() || !userTemplateOpt.get().getUserId().equals(auth.getName())) {
                return ResponseEntity.badRequest().body(Result.failed("无权查看他人的模板审核历史"));
            }
        }

        Result<List<AuditLog>> result = userTemplateService.getTemplateAuditHistory(id);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}