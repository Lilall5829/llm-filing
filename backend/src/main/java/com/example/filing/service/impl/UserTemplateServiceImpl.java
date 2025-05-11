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
import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;
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

    @Override
    public Result<Page<UserTemplate>> getUserTemplateList(String userId, Integer current, Integer pageSize) {
        try {
            PageRequest pageRequest = PageRequest.of(
                    current == null ? 0 : current - 1,
                    pageSize == null ? 10 : pageSize,
                    Sort.by("createTime").descending());

            Page<UserTemplate> userTemplates = userTemplateRepository.findByUserId(userId, pageRequest);
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

            // 验证用户是否存在
            List<SysUser> users = sysUserRepository.findAllById(userIds);
            if (users.isEmpty() || users.size() != userIds.size()) {
                return Result.failed("部分用户不存在");
            }

            List<UserTemplate> newRelations = new ArrayList<>();

            // 创建用户模板关系
            for (String userId : userIds) {
                UserTemplate userTemplate = new UserTemplate();
                userTemplate.setUserId(userId);
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

            // 验证普通用户只能操作自己的模板
            if (!isAdmin && !userTemplate.getUserId().equals(userId)) {
                return Result.failed("无权操作他人的模板");
            }

            // 更新状态和备注
            userTemplate.setStatus(status);
            if (remarks != null && !remarks.isEmpty()) {
                userTemplate.setRemarks(remarks);
            }

            userTemplateRepository.save(userTemplate);

            return Result.success("状态更新成功");
        } catch (Exception e) {
            log.error("更新模板状态失败", e);
            return Result.failed("更新模板状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> getTemplateContent(String id) {
        try {
            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                return Result.failed("用户模板关系不存在");
            }

            UserTemplate userTemplate = userTemplateOpt.get();
            return Result.success(userTemplate.getContent());
        } catch (Exception e) {
            log.error("获取模板内容失败", e);
            return Result.failed("获取模板内容失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> saveTemplateContent(String id, String content, String userId) {
        try {
            Optional<UserTemplate> userTemplateOpt = userTemplateRepository.findById(id);
            if (!userTemplateOpt.isPresent()) {
                return Result.failed("用户模板关系不存在");
            }

            UserTemplate userTemplate = userTemplateOpt.get();

            // 验证用户只能操作自己的模板
            if (!userTemplate.getUserId().equals(userId)) {
                return Result.failed("无权操作他人的模板");
            }

            // 更新内容
            userTemplate.setContent(content);

            // 如果当前状态为待填写(3)，自动变更为填写中(4)
            if (userTemplate.getStatus() == UserTemplateStatus.PENDING_FILL) {
                userTemplate.setStatus(UserTemplateStatus.FILLING);
            }

            userTemplateRepository.save(userTemplate);

            return Result.success("内容保存成功");
        } catch (Exception e) {
            log.error("保存模板内容失败", e);
            return Result.failed("保存模板内容失败: " + e.getMessage());
        }
    }
}