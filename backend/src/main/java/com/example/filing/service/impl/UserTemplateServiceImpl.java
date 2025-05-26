package com.example.filing.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.entity.AuditLog;
import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.AuditLogRepository;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.service.UserTemplateService;
import com.example.filing.util.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户模板关系服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserTemplateServiceImpl implements UserTemplateService {

    private final UserTemplateRepository userTemplateRepository;
    private final TemplateRegistryRepository templateRegistryRepository;
    private final SysUserRepository sysUserRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    public Result<Page<UserTemplate>> getUserTemplateList(String userId, Integer current, Integer pageSize) {
        try {
            // 如果用户ID是loginName而不是UUID，先转换
            String userUUID = userId;
            if (!userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                log.debug("userId不是UUID格式，尝试按loginName查找用户: {}", userId);
                SysUser user = sysUserRepository.findByLoginName(userId);
                if (user != null) {
                    userUUID = user.getId();
                    log.debug("找到用户，转换userId为UUID: loginName={}, UUID={}", userId, userUUID);
                } else {
                    log.error("无法找到用户: loginName={}", userId);
                    return Result.failed("用户不存在");
                }
            }

            PageRequest pageRequest = PageRequest.of(
                    current == null ? 0 : current - 1,
                    pageSize == null ? 10 : pageSize,
                    Sort.by("createTime").descending());

            Page<UserTemplate> userTemplates = userTemplateRepository.findByUserId(userUUID, pageRequest);
            return Result.success(userTemplates);
        } catch (Exception e) {
            log.error("获取用户模板列表失败", e);
            return Result.failed("获取用户模板列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<List<String>> applyTemplate(String templateId, List<String> userIds, String operatorId,
            boolean isAdmin) {
        try {
            // 验证模板是否存在
            Optional<TemplateRegistry> templateOpt = templateRegistryRepository.findById(templateId);
            if (!templateOpt.isPresent()) {
                return Result.failed("模板不存在");
            }

            // 检查userIds是否为空，如果为空则将当前操作人作为用户ID
            if (userIds == null || userIds.isEmpty()) {
                userIds = new ArrayList<>();
                userIds.add(operatorId);
            }

            // 先处理userIds中的登录名，转换为UUID
            List<String> processedUserIds = new ArrayList<>();
            for (String userId : userIds) {
                String userUUID = userId;
                // 如果不是UUID格式，尝试按loginName查找用户
                if (!userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                    log.debug("userIds中的ID不是UUID格式，尝试按loginName查找用户: {}", userId);
                    SysUser user = sysUserRepository.findByLoginName(userId);
                    if (user != null) {
                        userUUID = user.getId();
                        log.debug("找到用户，转换userId为UUID: loginName={}, UUID={}", userId, userUUID);
                    } else {
                        // 如果没找到用户，记录错误并继续
                        log.error("在userIds中无法找到用户: loginName={}", userId);
                        return Result.failed("用户 " + userId + " 不存在");
                    }
                }
                processedUserIds.add(userUUID);
            }

            // 使用处理后的UUID列表查询用户
            List<SysUser> users = sysUserRepository.findAllById(processedUserIds);
            if (users.isEmpty() || users.size() != processedUserIds.size()) {
                return Result.failed("部分用户不存在");
            }

            // 如果操作人ID是loginName而不是UUID，先转换
            String operatorUUID = operatorId;
            if (!operatorId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                log.debug("操作人ID不是UUID格式，尝试按loginName查找用户: {}", operatorId);
                SysUser user = sysUserRepository.findByLoginName(operatorId);
                if (user != null) {
                    operatorUUID = user.getId();
                    log.debug("找到操作人，转换operatorId为UUID: loginName={}, UUID={}", operatorId, operatorUUID);
                }
            }

            // 获取操作人信息
            SysUser operator = sysUserRepository.findById(operatorUUID).orElse(null);
            String operatorName = operator != null ? operator.getUserName() : "未知用户";

            List<UserTemplate> newRelations = new ArrayList<>();

            // 创建用户模板关系，使用处理后的UUID列表
            for (String userUUID : processedUserIds) {
                UserTemplate userTemplate = new UserTemplate();
                userTemplate.setUserId(userUUID);
                userTemplate.setTemplateId(templateId);

                // 根据操作人类型设置初始状态
                if (isAdmin) {
                    // 管理员发送模板：设置为待填写(3)
                    userTemplate.setStatus(UserTemplateStatus.PENDING_FILL);
                    userTemplate.setRemarks("管理员发送模板");
                } else {
                    // 用户申请模板：设置为待审核(0)
                    userTemplate.setStatus(UserTemplateStatus.PENDING_APPROVAL);
                    userTemplate.setRemarks("用户申请模板，等待审核");
                }

                newRelations.add(userTemplate);
            }

            // 批量保存关系
            List<UserTemplate> savedRelations = userTemplateRepository.saveAll(newRelations);

            // 记录审核日志
            for (UserTemplate userTemplate : savedRelations) {
                createAuditLog(
                        "user_template",
                        userTemplate.getId(),
                        "创建关系",
                        null,
                        userTemplate.getStatus(),
                        userTemplate.getRemarks(),
                        operatorUUID,
                        operatorName,
                        isAdmin);
            }

            // 返回创建的关系ID列表
            List<String> relationIds = savedRelations.stream()
                    .map(UserTemplate::getId)
                    .collect(Collectors.toList());

            return Result.success(isAdmin ? "模板发送成功" : "模板申请成功", relationIds);
        } catch (Exception e) {
            log.error("申请/发送模板失败", e);
            return Result.failed("申请/发送模板失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> updateTemplateStatus(String id, Integer status, String remarks, String userId,
            boolean isAdmin) {
        try {
            // 验证用户模板关系是否存在
            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                return Result.failed("用户模板关系不存在");
            }

            UserTemplate userTemplate = userTemplateOpt.get();

            // 验证状态转换是否允许
            if (!UserTemplateStatus.isStatusTransitionAllowed(userTemplate.getStatus(), status, isAdmin)) {
                return Result.failed("当前状态不允许变更为目标状态");
            }

            // 如果用户ID是loginName而不是UUID，先转换
            String userUUID = userId;
            if (!isAdmin && !userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                log.debug("userId不是UUID格式，尝试按loginName查找用户: {}", userId);
                SysUser user = sysUserRepository.findByLoginName(userId);
                if (user != null) {
                    userUUID = user.getId();
                    log.debug("找到用户，转换userId为UUID: loginName={}, UUID={}", userId, userUUID);
                } else {
                    log.error("无法找到用户: loginName={}", userId);
                    return Result.failed("用户不存在");
                }
            }

            // 验证普通用户只能操作自己的模板
            if (!isAdmin && !userTemplate.getUserId().equals(userUUID)) {
                return Result.failed("无权操作他人的模板");
            }

            // 记录转换前的旧状态，用于审核记录
            int oldStatus = userTemplate.getStatus();

            // 更新状态和备注
            userTemplate.setStatus(status);

            // 构建备注信息，包含状态变更信息和用户提供的备注
            StringBuilder remarkBuilder = new StringBuilder();

            // 添加状态变更信息
            remarkBuilder.append("状态从【").append(getStatusDescription(oldStatus))
                    .append("】变更为【").append(getStatusDescription(status)).append("】");

            // 添加操作者信息
            if (isAdmin) {
                remarkBuilder.append("，管理员操作");

                // 补充不同审核状态下的默认说明
                if (status == UserTemplateStatus.REVIEW_APPROVED) {
                    remarkBuilder.append("，审核通过");
                } else if (status == UserTemplateStatus.RETURNED) {
                    remarkBuilder.append("，退回修改");
                } else if (status == UserTemplateStatus.APPLICATION_APPROVED) {
                    remarkBuilder.append("，申请通过");
                } else if (status == UserTemplateStatus.APPLICATION_REJECTED) {
                    remarkBuilder.append("，拒绝申请");
                }
            } else {
                remarkBuilder.append("，用户操作");

                // 如果是用户提交审核
                if (oldStatus == UserTemplateStatus.FILLING && status == UserTemplateStatus.UNDER_REVIEW) {
                    remarkBuilder.append("，提交审核");
                }
            }

            // 添加用户提供的具体备注信息
            if (remarks != null && !remarks.isEmpty()) {
                remarkBuilder.append("，备注：").append(remarks);
            }

            // 设置完整的备注信息
            String fullRemarks = remarkBuilder.toString();
            userTemplate.setRemarks(fullRemarks);

            // 保存更新
            userTemplateRepository.save(userTemplate);

            // 获取操作人信息
            SysUser operator = sysUserRepository.findById(userUUID).orElse(null);
            String operatorName = operator != null ? operator.getUserName() : "未知用户";

            // 记录审核日志
            createAuditLog(
                    "user_template",
                    id,
                    "状态变更",
                    oldStatus,
                    status,
                    fullRemarks,
                    userUUID,
                    operatorName,
                    isAdmin);

            // 如果需要，可以在这里添加发送通知的逻辑
            sendStatusChangeNotification(userTemplate, oldStatus, status);

            return Result.success("状态更新成功");
        } catch (Exception e) {
            log.error("更新模板状态失败", e);
            return Result.failed("更新模板状态失败: " + e.getMessage());
        }
    }

    /**
     * 创建审核日志
     */
    private void createAuditLog(String entityType, String entityId, String operationType,
            Integer oldStatus, Integer newStatus, String details,
            String operatorId, String operatorName, boolean isAdmin) {
        try {
            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setOperationType(operationType);
            log.setOldStatus(oldStatus);
            log.setNewStatus(newStatus);
            log.setDetails(details);
            log.setOperatorId(operatorId);
            log.setOperatorName(operatorName);
            log.setAdmin(isAdmin);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // 记录日志失败不应影响主要业务流程，仅记录错误
            log.error("记录审核日志失败", e);
        }
    }

    /**
     * 获取状态描述文本
     * 
     * @param status 状态码
     * @return 状态描述
     */
    private String getStatusDescription(int status) {
        switch (status) {
            case UserTemplateStatus.PENDING_APPROVAL:
                return "待审核";
            case UserTemplateStatus.APPLICATION_APPROVED:
                return "申请通过";
            case UserTemplateStatus.APPLICATION_REJECTED:
                return "拒绝申请";
            case UserTemplateStatus.PENDING_FILL:
                return "待填写";
            case UserTemplateStatus.FILLING:
                return "填写中";
            case UserTemplateStatus.UNDER_REVIEW:
                return "审核中";
            case UserTemplateStatus.REVIEW_APPROVED:
                return "审核通过";
            case UserTemplateStatus.RETURNED:
                return "退回";
            default:
                return "未知状态";
        }
    }

    /**
     * 发送状态变更通知
     * 
     * @param userTemplate 用户模板关系
     * @param oldStatus    旧状态
     * @param newStatus    新状态
     */
    private void sendStatusChangeNotification(UserTemplate userTemplate, int oldStatus, int newStatus) {
        // 这里实现通知逻辑，可以是邮件、短信或站内消息等
        log.info("模板状态变更通知: 用户ID={}, 模板ID={}, 旧状态={}, 新状态={}",
                userTemplate.getUserId(), userTemplate.getTemplateId(),
                getStatusDescription(oldStatus), getStatusDescription(newStatus));

        // TODO: 实现实际的通知发送逻辑
        // 例如：如果状态变更为审核通过或退回，可以通知用户
        if (newStatus == UserTemplateStatus.REVIEW_APPROVED || newStatus == UserTemplateStatus.RETURNED) {
            // notificationService.notifyUser(userTemplate.getUserId(), "模板审核结果通知",
            // "您的模板已" + (newStatus == UserTemplateStatus.REVIEW_APPROVED ? "审核通过" :
            // "被退回，请修改"));
        }
    }

    @Override
    public Result<String> getTemplateContent(String id) {
        try {
            log.debug("获取模板内容: id={}", id);

            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                log.error("用户模板关系不存在: id={}", id);
                return Result.failed("用户模板关系不存在");
            }

            UserTemplate userTemplate = userTemplateOpt.get();
            log.debug("找到用户模板: id={}, userId={}, templateId={}, status={}",
                    userTemplate.getId(), userTemplate.getUserId(),
                    userTemplate.getTemplateId(), userTemplate.getStatus());

            String content = userTemplate.getContent();
            log.debug("模板内容: {}", content);

            // 如果内容为空，返回空对象而不是null
            if (content == null || content.trim().isEmpty()) {
                log.info("模板内容为空，返回空JSON对象");
                content = "{}"; // 返回空JSON对象
            }

            log.debug("返回模板内容，长度: {}", content.length());
            return Result.success(content);
        } catch (Exception e) {
            log.error("获取模板内容失败", e);
            return Result.failed("获取模板内容失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> saveTemplateContent(String id, String content, String userId) {
        try {
            log.debug("开始保存模板内容: id={}, userId={}, content={}", id, userId, content);

            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                log.error("用户模板关系不存在: id={}", id);
                return Result.failed("用户模板关系不存在");
            }

            UserTemplate userTemplate = userTemplateOpt.get();
            log.debug("找到用户模板: id={}, userId={}, templateId={}, status={}",
                    userTemplate.getId(), userTemplate.getUserId(),
                    userTemplate.getTemplateId(), userTemplate.getStatus());

            // 验证用户只能操作自己的模板
            // 首先检查传入的userId是否是loginName而不是UUID
            String userUUID = userId;
            if (!userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                log.debug("userId不是UUID格式，尝试按loginName查找用户: {}", userId);
                SysUser user = sysUserRepository.findByLoginName(userId);
                if (user != null) {
                    userUUID = user.getId();
                    log.debug("找到用户，转换userId为UUID: loginName={}, UUID={}", userId, userUUID);
                } else {
                    log.error("无法找到用户: loginName={}", userId);
                    return Result.failed("用户不存在");
                }
            }

            if (!userTemplate.getUserId().equals(userUUID)) {
                log.error("无权操作他人的模板: 模板userId={}, 当前userId={}", userTemplate.getUserId(), userUUID);
                return Result.failed("无权操作他人的模板");
            }

            // 记录旧状态
            int oldStatus = userTemplate.getStatus();
            boolean statusChanged = false;

            // 更新内容
            try {
                log.debug("尝试解析并设置内容: {}", content);
                userTemplate.setContent(content);
            } catch (Exception e) {
                log.error("设置内容失败，可能是格式不正确: {}", e.getMessage(), e);
                return Result.failed("内容格式不正确: " + e.getMessage());
            }

            // 如果当前状态为待填写(3)，自动变更为填写中(4)
            if (userTemplate.getStatus() == UserTemplateStatus.PENDING_FILL) {
                log.debug("状态从待填写变更为填写中");
                userTemplate.setStatus(UserTemplateStatus.FILLING);
                statusChanged = true;
            }

            // 保存更新
            try {
                userTemplateRepository.save(userTemplate);
                log.debug("成功保存用户模板");
            } catch (Exception e) {
                log.error("保存到数据库失败: {}", e.getMessage(), e);
                return Result.failed("保存到数据库失败: " + e.getMessage());
            }

            // 获取操作人信息
            SysUser operator = sysUserRepository.findById(userUUID).orElse(null);
            String operatorName = operator != null ? operator.getUserName() : "未知用户";

            // 记录内容更新日志
            try {
                createAuditLog(
                        "user_template",
                        id,
                        "内容更新",
                        oldStatus,
                        userTemplate.getStatus(),
                        statusChanged ? "内容更新并状态变更为填写中" : "内容更新",
                        userUUID,
                        operatorName,
                        false);
                log.debug("成功创建审核日志");
            } catch (Exception e) {
                log.warn("创建审核日志失败，但不影响主流程: {}", e.getMessage());
            }

            log.debug("模板内容保存成功");
            return Result.success("内容保存成功");
        } catch (Exception e) {
            log.error("保存模板内容失败: {}", e.getMessage(), e);
            return Result.failed("保存模板内容失败: " + e.getMessage());
        }
    }

    /**
     * 获取模板审核历史
     *
     * @param id 用户模板关系ID
     * @return 审核历史列表
     */
    @Override
    public Result<List<AuditLog>> getTemplateAuditHistory(String id) {
        try {
            // 验证用户模板关系是否存在
            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                return Result.failed("用户模板关系不存在");
            }

            // 获取审核历史日志
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                    "user_template", id);

            return Result.success(auditLogs);
        } catch (Exception e) {
            log.error("获取模板审核历史失败", e);
            return Result.failed("获取模板审核历史失败: " + e.getMessage());
        }
    }
}