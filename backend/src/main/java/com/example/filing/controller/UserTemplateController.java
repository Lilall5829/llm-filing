package com.example.filing.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import com.example.filing.dto.response.UserTemplateDTO;
import com.example.filing.entity.AuditLog;
import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.repository.TemplateRegistryRepository;
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
    private final SysUserRepository sysUserRepository;
    private final TemplateRegistryRepository templateRegistryRepository;

    /**
     * 分页获取用户模板列表
     *
     * @param userId       用户ID (可选，管理员可不传获取所有用户模板)
     * @param templateName 模板名称（可选，筛选条件）
     * @param templateCode 模板编号（可选，筛选条件）
     * @param status       状态值（可选，筛选条件）
     * @param id           模板ID（可选，用于查询单条记录）
     * @param pageNum      页码（从1开始，前端参数）
     * @param pageSize     每页大小（前端参数）
     * @return 用户模板分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Page<UserTemplateDTO>>> getUserTemplateList(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String id,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            Authentication auth) {

        try {
            // 获取当前用户登录名(loginName)和角色
            String loginName = auth.getName();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            System.out.println("获取用户模板列表 - 请求参数: userId=" + userId + ", templateName=" + templateName +
                    ", templateCode=" + templateCode + ", status=" + status + ", id=" + id +
                    ", pageNum=" + pageNum + ", pageSize=" + pageSize);
            System.out.println("获取用户模板列表 - 认证信息: loginName=" + loginName + ", isAdmin=" + isAdmin);

            // 通过登录名获取用户ID(UUID)
            SysUser currentUser = sysUserRepository.findByLoginName(loginName);
            if (currentUser == null) {
                System.err.println("错误: 找不到登录名为 " + loginName + " 的用户");
                return ResponseEntity.badRequest()
                        .body(Result.failed("无法识别当前用户"));
            }

            String currentUserId = currentUser.getId();
            System.out.println("当前用户信息: loginName=" + loginName + ", userId=" + currentUserId);

            // 处理根据ID查询单条记录的情况
            if (id != null && !id.isEmpty()) {
                System.out.println("正在根据ID查询单个模板: id=" + id);

                // 获取模板记录
                Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);

                if (!userTemplateOpt.isPresent()) {
                    System.out.println("未找到ID为 " + id + " 的模板记录");
                    // 返回空的分页结果
                    Page<UserTemplateDTO> emptyPage = Page.empty();
                    return ResponseEntity.ok(Result.success(emptyPage));
                }

                UserTemplate userTemplate = userTemplateOpt.get();

                // 普通用户只能查看自己的记录
                if (!isAdmin && !userTemplate.getUserId().equals(currentUserId)) {
                    System.out.println("权限错误: 普通用户尝试查看其他用户的模板, userId=" + userTemplate.getUserId());
                    return ResponseEntity.badRequest()
                            .body(Result.failed("无权查看此记录"));
                }

                // 获取关联的模板信息
                Optional<TemplateRegistry> templateOpt = templateRegistryRepository
                        .findById(userTemplate.getTemplateId());
                TemplateRegistry template = templateOpt.orElse(null);

                // 获取关联的用户信息
                Optional<SysUser> userOpt = sysUserRepository.findById(userTemplate.getUserId());
                SysUser user = userOpt.orElse(null);

                // 转换为DTO
                UserTemplateDTO dto = UserTemplateDTO.fromEntity(userTemplate, template, user);

                // 创建包含单条记录的分页结果
                List<UserTemplateDTO> singleResult = new ArrayList<>();
                singleResult.add(dto);

                // 前端pageNum从1开始，后端page从0开始，需要转换
                int page = pageNum > 0 ? pageNum - 1 : 0;
                PageRequest pageRequest = PageRequest.of(page, pageSize);

                Page<UserTemplateDTO> resultPage = new PageImpl<>(
                        singleResult, pageRequest, 1);

                System.out.println("成功查询到模板记录: id=" + userTemplate.getId());
                return ResponseEntity.ok(Result.success(resultPage));
            }

            // 前端pageNum从1开始，后端page从0开始，需要转换
            int page = pageNum > 0 ? pageNum - 1 : 0;

            PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by("createTime").descending());
            Page<Object[]> queryResult;

            // 处理特殊值"current"，用当前登录用户ID替代
            if ("current".equals(userId)) {
                System.out.println("检测到userId=current, 替换为当前用户ID: " + currentUserId);
                userId = currentUserId;
            }

            // 管理员特殊处理：不传userId或userId为'all'时，返回所有模板
            if (isAdmin && (userId == null || "all".equals(userId))) {
                System.out.println("管理员查询所有用户模板，过滤条件: templateName=" + templateName +
                        ", templateCode=" + templateCode + ", status=" + status);

                // 使用新的联合查询方法，同时获取模板信息
                queryResult = userTemplateRepository.findAllWithTemplateInfoFilters(
                        pageRequest,
                        templateName,
                        templateCode,
                        status);

                System.out.println("查询结果: 总记录数=" + queryResult.getTotalElements() +
                        ", 当前页记录数=" + queryResult.getContent().size());
            } else {
                // 非管理员或管理员查询特定用户
                // 如果userId为null且非管理员，使用当前用户的ID
                String targetUserId = userId != null ? userId : currentUserId;
                System.out.println("查询特定用户的模板: targetUserId=" + targetUserId);

                // 只能查自己的（非管理员情况下）
                if (!isAdmin && !targetUserId.equals(currentUserId)) {
                    System.out.println("权限错误: 非管理员尝试查看其他用户的模板");
                    return ResponseEntity.badRequest()
                            .body(Result.failed("无权查看其他用户的模板"));
                }

                // 使用新的带模板信息的查询方法
                queryResult = userTemplateRepository.findByUserIdWithTemplateInfoFilters(
                        targetUserId,
                        pageRequest,
                        templateName,
                        templateCode,
                        status);

                System.out.println("查询结果: 总记录数=" + queryResult.getTotalElements() +
                        ", 当前页记录数=" + queryResult.getContent().size());

                // 如果结果为空，检查是否可能是因为数据库中没有记录
                if (queryResult.getTotalElements() == 0) {
                    System.out.println("记录为空，检查数据库中是否存在用户ID为 " + targetUserId + " 的记录");

                    // 尝试查询数据库中是否有此用户的记录
                    long count = userTemplateRepository.count();
                    System.out.println("数据库中总记录数: " + count);

                    if (count > 0) {
                        // 如果有记录但查询结果为空，可能是权限问题或查询条件问题
                        System.out.println("数据库中有记录，但当前用户没有匹配记录，可能原因:");
                        System.out.println("1. 用户ID不匹配 - 当前查询的用户ID: " + targetUserId);
                        System.out.println(
                                "2. 查询条件过滤 - 模板名称: " + templateName + ", 模板编号: " + templateCode + ", 状态: " + status);
                    } else {
                        System.out.println("数据库中没有任何用户模板记录，需要先创建记录");
                    }
                } else {
                    // 打印出查询到的记录详情，帮助调试
                    System.out.println("查询到的记录详情:");
                    int printLimit = Math.min(queryResult.getContent().size(), 3); // 最多输出3条避免日志过长
                    for (int i = 0; i < printLimit; i++) {
                        Object[] result = queryResult.getContent().get(i);
                        UserTemplate ut = (UserTemplate) result[0];
                        TemplateRegistry tr = (TemplateRegistry) result[1];
                        System.out.println("记录 #" + i + ": ID=" + ut.getId() +
                                ", 用户ID=" + ut.getUserId() +
                                ", 模板ID=" + ut.getTemplateId() +
                                ", 模板名称=" + tr.getTemplateName() +
                                ", 模板编号=" + tr.getTemplateCode() +
                                ", 状态=" + ut.getStatus() +
                                ", 创建时间=" + ut.getCreateTime());
                    }
                }
            }

            // 将查询结果转换为DTO
            List<UserTemplateDTO> dtoList = queryResult.getContent().stream()
                    .map(result -> {
                        UserTemplate ut = (UserTemplate) result[0];
                        TemplateRegistry tr = (TemplateRegistry) result[1];
                        SysUser su = result.length > 2 ? (SysUser) result[2] : null; // 处理可能没有用户信息的情况
                        return UserTemplateDTO.fromEntity(ut, tr, su);
                    })
                    .collect(Collectors.toList());

            // 创建DTO分页结果
            Page<UserTemplateDTO> dtoPage = new PageImpl<>(
                    dtoList,
                    queryResult.getPageable(),
                    queryResult.getTotalElements());

            // 使用 Result.success 包装返回结果
            return ResponseEntity.ok(Result.success(dtoPage));
        } catch (Exception e) {
            System.err.println("获取用户模板列表失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Result.failed("获取用户模板列表失败: " + e.getMessage()));
        }
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

        try {
            // 获取当前用户ID和角色
            String userId = auth.getName();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            System.out.println("申请模板 - 认证信息: userId=" + userId + ", isAdmin=" + isAdmin);
            System.out.println("申请模板 - 请求参数: templateId=" + templateId);
            System.out.println("申请模板 - 请求体: request=" + (request == null ? "null" : request));

            // 修改：检查并处理userIds为空的情况
            List<String> userIdList;
            if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
                // 如果userIds为空，则使用当前登录用户
                userIdList = new ArrayList<>();
                userIdList.add(userId);
                System.out.println("申请模板 - 使用当前用户ID: " + userId);
            } else {
                // 否则使用传入的userIds
                userIdList = request.getUserIds();
                System.out.println("申请模板 - 使用请求中的userIds: " + userIdList);
            }

            // 调用服务层方法
            Result<List<String>> result = userTemplateService.applyTemplate(
                    templateId, userIdList, userId, isAdmin);

            System.out.println("申请模板 - 服务层返回结果: " + result);

            if (result.getCode() == 200) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            System.err.println("申请模板 - 发生异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.failed("申请模板失败: " + e.getMessage()));
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

        // 添加调试日志
        System.out.println("提交审核 - 接收请求: id=" + id);
        System.out.println("提交审核 - 认证用户: " + auth.getName());

        try {
            // 从Authentication获取用户登录名
            String loginName = auth.getName();

            // 通过登录名查找用户实体以获取UUID
            SysUser user = sysUserRepository.findByLoginName(loginName);
            if (user == null) {
                System.err.println("错误: 找不到登录名为 " + loginName + " 的用户");
                return ResponseEntity.badRequest()
                        .body(Result.failed("无法识别当前用户"));
            }

            // 获取用户ID(UUID)
            String userUUID = user.getId();
            System.out.println("提交审核 - 用户信息: loginName=" + loginName + ", userId=" + userUUID);

            // 验证模板是否存在且状态是否允许提交
            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                System.out.println("提交审核 - 模板不存在: id=" + id);
                return ResponseEntity.badRequest().body(Result.failed("模板不存在"));
            }

            UserTemplate userTemplate = userTemplateOpt.get();
            System.out.println(
                    "提交审核 - 找到模板: userId=" + userTemplate.getUserId() + ", status=" + userTemplate.getStatus());

            // 验证是否是用户自己的模板，使用UUID比较
            if (!userTemplate.getUserId().equals(userUUID)) {
                System.out.println("提交审核 - 无权操作: 模板用户ID=" + userTemplate.getUserId() + ", 当前用户ID=" + userUUID);
                return ResponseEntity.badRequest().body(Result.failed("无权操作他人的模板"));
            }

            // 验证模板状态是否允许提交审核（必须是填写中(4)或退回(7)状态）
            int currentStatus = userTemplate.getStatus();
            if (currentStatus != UserTemplateStatus.FILLING && currentStatus != UserTemplateStatus.RETURNED) {
                String statusDesc = currentStatus == UserTemplateStatus.UNDER_REVIEW ? "审核中"
                        : currentStatus == UserTemplateStatus.REVIEW_APPROVED ? "已审核通过" : "当前状态";
                System.out.println("提交审核 - 状态不允许提交: 当前状态=" + currentStatus + " (" + statusDesc + ")");
                return ResponseEntity.badRequest().body(Result.failed("模板已是" + statusDesc + "，不能提交审核"));
            }

            // 验证模板是否有内容
            if (userTemplate.getContent() == null || userTemplate.getContent().isEmpty()) {
                System.out.println("提交审核 - 模板内容为空");
                return ResponseEntity.badRequest().body(Result.failed("模板内容不能为空，请先填写内容"));
            }

            // 调用服务层方法，将状态设为审核中(5)
            // 注意: 此处传递loginName而非UUID，因为服务层会再次进行转换
            System.out.println("提交审核 - 调用服务层: id=" + id + ", loginName=" + loginName);
            Result<String> result = userTemplateService.updateTemplateStatus(
                    id, UserTemplateStatus.UNDER_REVIEW, "用户提交审核", loginName, false);

            System.out.println("提交审核 - 服务层返回: code=" + result.getCode() + ", message=" + result.getMessage());

            if (result.getCode() == 200) {
                return ResponseEntity.ok(Result.success("已提交审核，请等待管理员审核"));
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            System.err.println("提交审核 - 处理异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.failed("提交审核失败: " + e.getMessage()));
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

        // 添加调试日志
        System.out.println("保存模板内容 - 接收请求: id=" + id);
        System.out.println("保存模板内容 - 认证用户: " + auth.getName());

        // 记录内容长度和前100个字符，避免日志过长
        int contentLength = content != null ? content.length() : 0;
        String contentPreview = content != null && contentLength > 0
                ? (contentLength > 100 ? content.substring(0, 100) + "..." : content)
                : "null或空字符串";
        System.out.println("保存模板内容 - 内容长度: " + contentLength + ", 内容预览: " + contentPreview);

        // 调用服务
        String userId = auth.getName();
        Result<String> result = userTemplateService.saveTemplateContent(id, content, userId);

        System.out.println("保存模板内容 - 处理结果: code=" + result.getCode() + ", message=" + result.getMessage());

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
            if (!userTemplateOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Result.failed("模板不存在"));
            }

            UserTemplate userTemplate = userTemplateOpt.get();
            String loginName = auth.getName();

            // 检查auth.getName()是否是loginName，如果是则查找对应的用户ID
            SysUser user = sysUserRepository.findByLoginName(loginName);
            if (user == null) {
                return ResponseEntity.badRequest().body(Result.failed("无法识别当前用户"));
            }

            // 比较用户ID
            if (!userTemplate.getUserId().equals(user.getId())) {
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

    /**
     * 获取模板统计信息
     * 
     * @param auth 认证信息
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Map<String, Object>>> getTemplateStatistics(Authentication auth) {
        try {
            // 获取当前用户ID和角色
            String currentUserId = auth.getName();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            System.out.println("开始获取统计数据: userId=" + currentUserId + ", isAdmin=" + isAdmin);

            // 统计数据
            Map<String, Object> statistics = new HashMap<>();

            try {
                // 获取总模板数
                long totalTemplates = templateRegistryRepository.countByDeletedFalse();
                statistics.put("totalTemplates", totalTemplates);
                System.out.println("总模板数: " + totalTemplates);
            } catch (Exception e) {
                System.err.println("获取总模板数失败: " + e.getMessage());
                e.printStackTrace();
                statistics.put("totalTemplates", 0);
            }

            try {
                // 获取待处理申请数量（仅状态为0-待审核的记录）
                long pendingApproval = userTemplateRepository.countByStatus(UserTemplateStatus.PENDING_APPROVAL);
                statistics.put("pendingCount", pendingApproval);
                System.out.println("待批准申请数量: " + pendingApproval);
            } catch (Exception e) {
                System.err.println("获取待处理申请数量失败: " + e.getMessage());
                e.printStackTrace();
                statistics.put("pendingCount", 0);
            }

            try {
                // 获取进行中的任务数量（状态为5-审核中的记录数）
                long underReview = userTemplateRepository.countByStatus(UserTemplateStatus.UNDER_REVIEW);
                statistics.put("inProgressCount", underReview);
                System.out.println("审核中的任务数量: " + underReview);
            } catch (Exception e) {
                System.err.println("获取审核中任务数量失败: " + e.getMessage());
                e.printStackTrace();
                statistics.put("inProgressCount", 0);
            }

            try {
                // 获取已完成备案数量（状态为6-审核通过的记录数）
                long approvedCount = userTemplateRepository.countByStatus(UserTemplateStatus.REVIEW_APPROVED);
                statistics.put("approvedCount", approvedCount);
                System.out.println("已完成备案数量: " + approvedCount);
            } catch (Exception e) {
                System.err.println("获取已完成备案数量失败: " + e.getMessage());
                e.printStackTrace();
                statistics.put("approvedCount", 0);
            }

            try {
                // 获取任务总数
                long totalTasks = userTemplateRepository.count();
                statistics.put("totalTasks", totalTasks);
                System.out.println("任务总数: " + totalTasks);
            } catch (Exception e) {
                System.err.println("获取任务总数失败: " + e.getMessage());
                e.printStackTrace();
                statistics.put("totalTasks", 0);
            }

            System.out.println("统计数据获取完成: " + statistics);
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            System.err.println("获取统计数据失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Result.failed("获取统计数据失败: " + e.getMessage()));
        }
    }

    /**
     * 调试用：列出所有用户模板记录
     * 
     * @return 所有用户模板记录
     */
    @GetMapping("/debug/listAll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<UserTemplate>>> listAllTemplates() {
        try {
            List<UserTemplate> allTemplates = userTemplateRepository.findAll();
            System.out.println("调试：找到的记录总数: " + allTemplates.size());

            // 打印每条记录的基本信息
            for (int i = 0; i < allTemplates.size(); i++) {
                UserTemplate ut = allTemplates.get(i);
                System.out.println("记录 #" + i + ": ID=" + ut.getId() +
                        ", 用户ID=" + ut.getUserId() +
                        ", 模板ID=" + ut.getTemplateId() +
                        ", 状态=" + ut.getStatus());
            }

            return ResponseEntity.ok(Result.success(allTemplates));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.failed("获取所有模板记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取模板定义（普通用户可访问）
     * 普通用户只能获取已分配给自己的模板定义
     *
     * @param templateId 模板ID
     * @param auth       认证信息
     * @return 模板定义
     */
    @GetMapping("/getTemplateDefinition")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<TemplateRegistry>> getTemplateDefinition(
            @RequestParam String templateId,
            Authentication auth) {

        try {
            // 获取当前用户登录名和角色
            String loginName = auth.getName();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            System.out.println("获取模板定义 - 认证信息: loginName=" + loginName + ", isAdmin=" + isAdmin);
            System.out.println("获取模板定义 - 请求参数: templateId=" + templateId);

            // 如果是管理员，直接返回模板定义
            if (isAdmin) {
                Optional<TemplateRegistry> templateOpt = templateRegistryRepository.findById(templateId);
                if (!templateOpt.isPresent()) {
                    return ResponseEntity.badRequest().body(Result.failed("模板不存在"));
                }
                return ResponseEntity.ok(Result.success(templateOpt.get()));
            }

            // 普通用户需要验证是否有权限访问此模板
            // 通过登录名获取用户ID
            SysUser currentUser = sysUserRepository.findByLoginName(loginName);
            if (currentUser == null) {
                System.err.println("错误: 找不到登录名为 " + loginName + " 的用户");
                return ResponseEntity.badRequest().body(Result.failed("无法识别当前用户"));
            }

            String currentUserId = currentUser.getId();
            System.out.println("当前用户信息: loginName=" + loginName + ", userId=" + currentUserId);

            // 检查用户是否有使用此模板的权限（是否存在用户模板关系）
            boolean hasPermission = userTemplateRepository.existsByUserIdAndTemplateId(currentUserId, templateId);

            if (!hasPermission) {
                System.out.println("权限错误: 用户没有访问模板的权限, userId=" + currentUserId + ", templateId=" + templateId);
                return ResponseEntity.badRequest().body(Result.failed("无权访问此模板"));
            }

            // 获取模板定义
            Optional<TemplateRegistry> templateOpt = templateRegistryRepository.findById(templateId);
            if (!templateOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Result.failed("模板不存在"));
            }

            TemplateRegistry template = templateOpt.get();

            // 检查模板是否被删除
            if (template.getDeleted() != null && template.getDeleted() == 1) {
                return ResponseEntity.badRequest().body(Result.failed("模板已被删除"));
            }

            System.out.println("成功返回模板定义: " + template.getTemplateName());
            return ResponseEntity.ok(Result.success(template));

        } catch (Exception e) {
            System.err.println("获取模板定义失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.failed("获取模板定义失败: " + e.getMessage()));
        }
    }
}