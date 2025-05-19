package com.example.filing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
import com.example.filing.util.Result;

/**
 * 用户模板审核流程测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserTemplateAuditTest {

        @Autowired
        private UserTemplateService userTemplateService;

        @Autowired
        private UserTemplateRepository userTemplateRepository;

        @Autowired
        private TemplateRegistryRepository templateRegistryRepository;

        @Autowired
        private SysUserRepository userRepository;

        @Autowired
        private AuditLogRepository auditLogRepository;

        private SysUser adminUser;
        private SysUser normalUser;
        private TemplateRegistry testTemplate;
        private UserTemplate userTemplate;

        @BeforeEach
        public void setup() {
                // 创建管理员用户
                adminUser = new SysUser();
                adminUser.setLoginName("admin_test");
                adminUser.setPassword("admin123");
                adminUser.setUserName("管理员测试");
                adminUser.setRole(1); // 管理员角色
                adminUser.setStatus(1); // 启用状态
                adminUser.setCreateTime(LocalDateTime.now());
                adminUser.setUpdateTime(LocalDateTime.now());
                adminUser = userRepository.save(adminUser);

                // 创建普通用户
                normalUser = new SysUser();
                normalUser.setLoginName("user_test");
                normalUser.setPassword("user123");
                normalUser.setUserName("用户测试");
                normalUser.setRole(2); // 普通用户角色
                normalUser.setStatus(1); // 启用状态
                normalUser.setCreateTime(LocalDateTime.now());
                normalUser.setUpdateTime(LocalDateTime.now());
                normalUser = userRepository.save(normalUser);

                // 创建测试模板
                testTemplate = new TemplateRegistry();
                testTemplate.setTemplateName("测试模板");
                testTemplate.setTemplateCode("TEST_TEMPLATE");
                testTemplate.setTemplateType("测试类型");
                testTemplate.setTemplateDescription("测试模板描述");
                testTemplate.setTemplateContent("{\"test\": \"content\"}");
                testTemplate.setDeleted(0);
                testTemplate.setCreateBy(adminUser.getId());
                testTemplate.setCreateTime(LocalDateTime.now());
                testTemplate.setUpdateTime(LocalDateTime.now());
                testTemplate = templateRegistryRepository.save(testTemplate);

                // 创建用户模板关系（设置为"填写中"状态，便于测试提交审核）
                userTemplate = new UserTemplate();
                userTemplate.setUserId(normalUser.getId());
                userTemplate.setTemplateId(testTemplate.getId());
                userTemplate.setContent("{\"data\": {\"field1\": \"value1\"}}");
                userTemplate.setStatus(UserTemplateStatus.FILLING); // 填写中状态
                userTemplate.setRemarks("初始测试数据");
                userTemplate.setCreateTime(LocalDateTime.now());
                userTemplate.setUpdateTime(LocalDateTime.now());
                userTemplate = userTemplateRepository.save(userTemplate);
        }

        @Test
        public void testUserSubmitForReview() {
                // 用户提交审核
                Result<String> result = userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "提交审核测试",
                                normalUser.getId(),
                                false);

                // 验证操作成功
                assertTrue(result.getCode() == 200, "提交审核应该成功");
                assertEquals("状态更新成功", result.getMessage());

                // 验证数据库中状态已更新
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
                assertTrue(updatedTemplate.isPresent(), "应该能找到更新后的模板");
                assertEquals(UserTemplateStatus.UNDER_REVIEW, updatedTemplate.get().getStatus(), "状态应该更新为审核中");
                assertTrue(updatedTemplate.get().getRemarks().contains("提交审核"), "备注中应该包含提交信息");

                // 验证审核日志已创建
                List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                                "user_template",
                                userTemplate.getId());
                assertFalse(logs.isEmpty(), "应该有审核日志记录");
                AuditLog latestLog = logs.get(0);
                assertEquals("状态变更", latestLog.getOperationType());
                assertEquals(UserTemplateStatus.FILLING, latestLog.getOldStatus());
                assertEquals(UserTemplateStatus.UNDER_REVIEW, latestLog.getNewStatus());
                assertEquals(normalUser.getId(), latestLog.getOperatorId());
                assertFalse(latestLog.isAdmin());
        }

        @Test
        public void testAdminApproveTemplate() {
                // 先将模板状态设为审核中
                userTemplate.setStatus(UserTemplateStatus.UNDER_REVIEW);
                userTemplate = userTemplateRepository.save(userTemplate);

                // 管理员审核通过
                Result<String> result = userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.REVIEW_APPROVED,
                                "审核通过测试",
                                adminUser.getId(),
                                true);

                // 验证操作成功
                assertTrue(result.getCode() == 200, "审核通过应该成功");
                assertEquals("状态更新成功", result.getMessage());

                // 验证数据库中状态已更新
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
                assertTrue(updatedTemplate.isPresent());
                assertEquals(UserTemplateStatus.REVIEW_APPROVED, updatedTemplate.get().getStatus(), "状态应该更新为审核通过");
                assertTrue(updatedTemplate.get().getRemarks().contains("审核通过"), "备注中应该包含审核信息");

                // 验证审核日志已创建
                List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                                "user_template",
                                userTemplate.getId());
                assertFalse(logs.isEmpty());
                AuditLog latestLog = logs.get(0);
                assertEquals("状态变更", latestLog.getOperationType());
                assertEquals(UserTemplateStatus.UNDER_REVIEW, latestLog.getOldStatus());
                assertEquals(UserTemplateStatus.REVIEW_APPROVED, latestLog.getNewStatus());
                assertEquals(adminUser.getId(), latestLog.getOperatorId());
                assertTrue(latestLog.isAdmin());
        }

        @Test
        public void testAdminRejectTemplate() {
                // 先将模板状态设为审核中
                userTemplate.setStatus(UserTemplateStatus.UNDER_REVIEW);
                userTemplate = userTemplateRepository.save(userTemplate);

                // 管理员退回模板
                Result<String> result = userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.RETURNED,
                                "需要修改内容",
                                adminUser.getId(),
                                true);

                // 验证操作成功
                assertTrue(result.getCode() == 200);
                assertEquals("状态更新成功", result.getMessage());

                // 验证数据库中状态已更新
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
                assertTrue(updatedTemplate.isPresent());
                assertEquals(UserTemplateStatus.RETURNED, updatedTemplate.get().getStatus(), "状态应该更新为退回");
                assertTrue(updatedTemplate.get().getRemarks().contains("需要修改内容"), "备注中应该包含退回原因");

                // 验证审核日志已创建
                List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                                "user_template",
                                userTemplate.getId());
                assertFalse(logs.isEmpty());
                AuditLog latestLog = logs.get(0);
                assertEquals("状态变更", latestLog.getOperationType());
                assertEquals(UserTemplateStatus.UNDER_REVIEW, latestLog.getOldStatus());
                assertEquals(UserTemplateStatus.RETURNED, latestLog.getNewStatus());
                assertEquals(adminUser.getId(), latestLog.getOperatorId());
                assertTrue(latestLog.isAdmin());
        }

        @Test
        public void testUserReSubmitAfterRejection() {
                // 先将模板状态设为退回
                userTemplate.setStatus(UserTemplateStatus.RETURNED);
                userTemplate = userTemplateRepository.save(userTemplate);

                // 用户修改后重新提交
                // 1. 保存内容
                Result<String> saveResult = userTemplateService.saveTemplateContent(
                                userTemplate.getId(),
                                "{\"data\": {\"field1\": \"updated value\"}}",
                                normalUser.getId());

                assertTrue(saveResult.getCode() == 200);

                // 2. 提交审核
                Result<String> submitResult = userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "已修改内容重新提交",
                                normalUser.getId(),
                                false);

                // 验证操作成功
                assertTrue(submitResult.getCode() == 200);
                assertEquals("状态更新成功", submitResult.getMessage());

                // 验证数据库中状态已更新
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
                assertTrue(updatedTemplate.isPresent());
                assertEquals(UserTemplateStatus.UNDER_REVIEW, updatedTemplate.get().getStatus());
                assertTrue(updatedTemplate.get().getRemarks().contains("已修改内容重新提交"));

                // 验证审核日志完整记录了整个流程
                List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                                "user_template",
                                userTemplate.getId());
                assertTrue(logs.size() >= 2, "应该有至少2条日志记录（内容更新和状态变更）");

                // 验证最新的两条日志
                assertEquals("状态变更", logs.get(0).getOperationType());
                assertEquals(UserTemplateStatus.RETURNED, logs.get(0).getOldStatus());
                assertEquals(UserTemplateStatus.UNDER_REVIEW, logs.get(0).getNewStatus());

                assertEquals("内容更新", logs.get(1).getOperationType());
        }

        @Test
        public void testGetAuditHistory() {
                // 先创建几条审核日志记录

                // 1. 用户提交审核
                userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "提交审核",
                                normalUser.getId(),
                                false);

                // 2. 管理员审核退回
                userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.RETURNED,
                                "需要修改某些内容",
                                adminUser.getId(),
                                true);

                // 3. 用户修改后再次提交
                userTemplateService.saveTemplateContent(
                                userTemplate.getId(),
                                "{\"data\": {\"field1\": \"updated again\"}}",
                                normalUser.getId());

                userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "修改完成重新提交",
                                normalUser.getId(),
                                false);

                // 4. 管理员审核通过
                userTemplateService.updateTemplateStatus(
                                userTemplate.getId(),
                                UserTemplateStatus.REVIEW_APPROVED,
                                "审核通过",
                                adminUser.getId(),
                                true);

                // 获取审核历史
                Result<List<AuditLog>> result = userTemplateService.getTemplateAuditHistory(userTemplate.getId());

                // 验证操作成功
                assertTrue(result.getCode() == 200, "获取审核历史应该成功");
                assertNotNull(result.getData(), "应返回审核历史数据");

                // 应该有至少5条记录（有可能更多，因为前面的测试可能也有记录）
                List<AuditLog> logs = result.getData();
                assertTrue(logs.size() >= 5, "审核历史应该包含至少5条记录");

                // 验证最新的记录是管理员审核通过
                AuditLog latestLog = logs.get(0);
                assertEquals("状态变更", latestLog.getOperationType());
                assertEquals(UserTemplateStatus.UNDER_REVIEW, latestLog.getOldStatus());
                assertEquals(UserTemplateStatus.REVIEW_APPROVED, latestLog.getNewStatus());
                assertEquals(adminUser.getId(), latestLog.getOperatorId());
                assertTrue(latestLog.isAdmin());

                // 验证完整的审核流程记录顺序
                if (logs.size() >= 5) {
                        // 最新的记录在前面，所以倒序查看是按照时间顺序的
                        assertEquals(UserTemplateStatus.REVIEW_APPROVED, logs.get(0).getNewStatus()); // 审核通过
                        assertEquals(UserTemplateStatus.UNDER_REVIEW, logs.get(1).getNewStatus()); // 重新提交
                        assertEquals("内容更新", logs.get(2).getOperationType()); // 内容更新
                        assertEquals(UserTemplateStatus.RETURNED, logs.get(3).getNewStatus()); // 退回
                        assertEquals(UserTemplateStatus.UNDER_REVIEW, logs.get(4).getNewStatus()); // 首次提交
                }
        }

        @Test
        public void testAdminReviewApplication() {
                // 创建一个待审核状态的模板申请
                UserTemplate applicationTemplate = new UserTemplate();
                applicationTemplate.setUserId(normalUser.getId());
                applicationTemplate.setTemplateId(testTemplate.getId());
                applicationTemplate.setStatus(UserTemplateStatus.PENDING_APPROVAL); // 待审核状态
                applicationTemplate.setRemarks("用户申请模板");
                applicationTemplate.setCreateTime(LocalDateTime.now());
                applicationTemplate.setUpdateTime(LocalDateTime.now());
                applicationTemplate = userTemplateRepository.save(applicationTemplate);

                // 管理员批准申请：从待审核(0)变更为待填写(3) - 符合新规则的有效操作
                Result<String> result = userTemplateService.updateTemplateStatus(
                                applicationTemplate.getId(),
                                UserTemplateStatus.PENDING_FILL, // 待填写状态
                                "同意申请",
                                adminUser.getId(),
                                true);

                // 验证操作成功
                assertTrue(result.getCode() == 200);

                // 验证数据库中状态已更新
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(applicationTemplate.getId());
                assertTrue(updatedTemplate.isPresent());
                assertEquals(UserTemplateStatus.PENDING_FILL, updatedTemplate.get().getStatus());
                assertTrue(updatedTemplate.get().getRemarks().contains("同意申请"));

                // 验证审核日志
                List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                                "user_template", applicationTemplate.getId());
                assertFalse(logs.isEmpty());
                assertEquals(UserTemplateStatus.PENDING_APPROVAL, logs.get(0).getOldStatus());
                assertEquals(UserTemplateStatus.PENDING_FILL, logs.get(0).getNewStatus());
        }

        @Test
        public void testAdminRejectApplication() {
                // 创建一个待审核状态的模板申请
                UserTemplate applicationTemplate = new UserTemplate();
                applicationTemplate.setUserId(normalUser.getId());
                applicationTemplate.setTemplateId(testTemplate.getId());
                applicationTemplate.setStatus(UserTemplateStatus.PENDING_APPROVAL); // 待审核状态
                applicationTemplate.setRemarks("用户申请模板");
                applicationTemplate.setCreateTime(LocalDateTime.now());
                applicationTemplate.setUpdateTime(LocalDateTime.now());
                applicationTemplate = userTemplateRepository.save(applicationTemplate);

                // 管理员拒绝申请：从待审核(0)变更为拒绝申请(2) - 符合新规则的有效操作
                Result<String> result = userTemplateService.updateTemplateStatus(
                                applicationTemplate.getId(),
                                UserTemplateStatus.APPLICATION_REJECTED, // 拒绝申请状态
                                "拒绝申请，原因：模板不适用",
                                adminUser.getId(),
                                true);

                // 验证操作成功
                assertTrue(result.getCode() == 200);

                // 验证数据库中状态已更新
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(applicationTemplate.getId());
                assertTrue(updatedTemplate.isPresent());
                assertEquals(UserTemplateStatus.APPLICATION_REJECTED, updatedTemplate.get().getStatus());
                assertTrue(updatedTemplate.get().getRemarks().contains("拒绝申请"));

                // 验证审核日志
                List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(
                                "user_template", applicationTemplate.getId());
                assertFalse(logs.isEmpty());
                assertEquals(UserTemplateStatus.PENDING_APPROVAL, logs.get(0).getOldStatus());
                assertEquals(UserTemplateStatus.APPLICATION_REJECTED, logs.get(0).getNewStatus());
        }

        @Test
        public void testAdminInvalidTransition() {
                // 创建一个待审核状态的模板申请
                UserTemplate applicationTemplate = new UserTemplate();
                applicationTemplate.setUserId(normalUser.getId());
                applicationTemplate.setTemplateId(testTemplate.getId());
                applicationTemplate.setStatus(UserTemplateStatus.PENDING_APPROVAL); // 待审核状态
                applicationTemplate.setRemarks("用户申请模板");
                applicationTemplate.setCreateTime(LocalDateTime.now());
                applicationTemplate.setUpdateTime(LocalDateTime.now());
                applicationTemplate = userTemplateRepository.save(applicationTemplate);

                // 管理员尝试无效操作：从待审核(0)直接变更为审核中(5) - 不符合新规则
                Result<String> result = userTemplateService.updateTemplateStatus(
                                applicationTemplate.getId(),
                                UserTemplateStatus.UNDER_REVIEW, // 审核中状态
                                "无效操作尝试",
                                adminUser.getId(),
                                true);

                // 验证操作失败
                assertTrue(result.getCode() == 500);
                assertEquals("当前状态不允许变更为目标状态", result.getMessage());

                // 验证数据库中状态未变更
                Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(applicationTemplate.getId());
                assertTrue(updatedTemplate.isPresent());
                assertEquals(UserTemplateStatus.PENDING_APPROVAL, updatedTemplate.get().getStatus());

                // 验证没有创建审核日志
                List<AuditLog> logs = auditLogRepository
                                .findByEntityTypeAndEntityIdAndOperationTypeOrderByOperationTimeDesc(
                                                "user_template", applicationTemplate.getId(), "状态变更");
                // 可能有其他类型的日志，但不应该有状态变更类型的
                boolean hasStatusChangeLog = false;
                for (AuditLog log : logs) {
                        if (log.getNewStatus() == UserTemplateStatus.UNDER_REVIEW) {
                                hasStatusChangeLog = true;
                                break;
                        }
                }
                assertFalse(hasStatusChangeLog, "不应该有状态变更为审核中的日志");
        }

        @Test
        public void testAuditLogPagination() {
                // Generate 10 audit log entries
                for (int i = 0; i < 10; i++) {
                        AuditLog log = new AuditLog();
                        log.setEntityType("user_template");
                        log.setEntityId(userTemplate.getId());
                        log.setOperationType("测试操作");
                        log.setOperatorId(adminUser.getId());
                        log.setOperatorName(adminUser.getUserName());
                        log.setAdmin(true);
                        log.setDetails("测试日志内容 " + i);
                        log.setOperationTime(LocalDateTime.now().minusMinutes(i)); // Different times
                        auditLogRepository.save(log);
                }

                // Test pagination - page 1 with 5 items per page
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5,
                                org.springframework.data.domain.Sort.by("operationTime").descending());

                org.springframework.data.domain.Page<AuditLog> page1 = auditLogRepository
                                .findByEntityTypeAndEntityId("user_template", userTemplate.getId(), pageable);

                assertEquals(5, page1.getContent().size(), "Page 1 should contain 5 items");
                assertTrue(page1.hasNext(), "Should have a next page");

                // Test page 2
                pageable = org.springframework.data.domain.PageRequest.of(1, 5,
                                org.springframework.data.domain.Sort.by("operationTime").descending());

                org.springframework.data.domain.Page<AuditLog> page2 = auditLogRepository
                                .findByEntityTypeAndEntityId("user_template", userTemplate.getId(), pageable);

                assertEquals(5, page2.getContent().size(), "Page 2 should contain 5 items");

                // Verify chronological order (newest first)
                assertTrue(
                                page1.getContent().get(0).getOperationTime()
                                                .isAfter(page2.getContent().get(0).getOperationTime()),
                                "Page 1 should contain newer entries than page 2");
        }

        @Test
        public void testAuditLogByOperationType() {
                // Create a few logs of different operation types
                AuditLog statusChangeLog = new AuditLog();
                statusChangeLog.setEntityType("user_template");
                statusChangeLog.setEntityId(userTemplate.getId());
                statusChangeLog.setOperationType("状态变更");
                statusChangeLog.setOperatorId(adminUser.getId());
                statusChangeLog.setAdmin(true);
                statusChangeLog.setDetails("状态变更测试");
                auditLogRepository.save(statusChangeLog);

                AuditLog contentUpdateLog = new AuditLog();
                contentUpdateLog.setEntityType("user_template");
                contentUpdateLog.setEntityId(userTemplate.getId());
                contentUpdateLog.setOperationType("内容更新");
                contentUpdateLog.setOperatorId(normalUser.getId());
                contentUpdateLog.setAdmin(false);
                contentUpdateLog.setDetails("内容更新测试");
                auditLogRepository.save(contentUpdateLog);

                // Retrieve logs filtered by operation type
                java.util.List<AuditLog> statusLogs = auditLogRepository.findByEntityTypeAndEntityIdAndOperationType(
                                "user_template", userTemplate.getId(), "状态变更");

                java.util.List<AuditLog> contentLogs = auditLogRepository.findByEntityTypeAndEntityIdAndOperationType(
                                "user_template", userTemplate.getId(), "内容更新");

                // Verify filtering works
                assertTrue(statusLogs.size() >= 1, "Should have at least one status change log");
                assertTrue(contentLogs.size() >= 1, "Should have at least one content update log");

                boolean foundStatusLog = false;
                for (AuditLog log : statusLogs) {
                        if (log.getDetails().equals("状态变更测试")) {
                                foundStatusLog = true;
                                break;
                        }
                }
                assertTrue(foundStatusLog, "Should find the specific status log");

                boolean foundContentLog = false;
                for (AuditLog log : contentLogs) {
                        if (log.getDetails().equals("内容更新测试")) {
                                foundContentLog = true;
                                break;
                        }
                }
                assertTrue(foundContentLog, "Should find the specific content log");
        }

        @Test
        public void testCompleteAuditFlow() {
                // Simulate a full lifecycle of a template with audit tracking:
                // 1. Admin sends template to user (PENDING_FILL)
                // 2. User fills out template (FILLING)
                // 3. User submits for review (UNDER_REVIEW)
                // 4. Admin rejects with feedback (RETURNED)
                // 5. User updates and resubmits (UNDER_REVIEW)
                // 6. Admin approves (REVIEW_APPROVED)

                // Create a new template for this specific test
                UserTemplate lifecycle = new UserTemplate();
                lifecycle.setUserId(normalUser.getId());
                lifecycle.setTemplateId(testTemplate.getId());
                lifecycle.setStatus(UserTemplateStatus.PENDING_FILL); // Start at PENDING_FILL
                lifecycle.setRemarks("管理员初始发送");
                lifecycle = userTemplateRepository.save(lifecycle);

                // 2. User fills and saves content
                Result<String> saveResult = userTemplateService.saveTemplateContent(
                                lifecycle.getId(),
                                "{\"data\": {\"field1\": \"initial value\"}}",
                                normalUser.getId());
                assertTrue(saveResult.getCode() == 200);

                // 3. User submits for review
                Result<String> submitResult = userTemplateService.updateTemplateStatus(
                                lifecycle.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "提交审核",
                                normalUser.getId(),
                                false);
                assertTrue(submitResult.getCode() == 200);

                // 4. Admin rejects
                Result<String> rejectResult = userTemplateService.updateTemplateStatus(
                                lifecycle.getId(),
                                UserTemplateStatus.RETURNED,
                                "需要修改：请补充更多信息",
                                adminUser.getId(),
                                true);
                assertTrue(rejectResult.getCode() == 200);

                // 5. User updates content and resubmits
                userTemplateService.saveTemplateContent(
                                lifecycle.getId(),
                                "{\"data\": {\"field1\": \"updated with more info\", \"field2\": \"additional info\"}}",
                                normalUser.getId());

                Result<String> resubmitResult = userTemplateService.updateTemplateStatus(
                                lifecycle.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "已修改并补充信息",
                                normalUser.getId(),
                                false);
                assertTrue(resubmitResult.getCode() == 200);

                // 6. Admin approves
                Result<String> approveResult = userTemplateService.updateTemplateStatus(
                                lifecycle.getId(),
                                UserTemplateStatus.REVIEW_APPROVED,
                                "审核通过",
                                adminUser.getId(),
                                true);
                assertTrue(approveResult.getCode() == 200);

                // Verify the audit history captures the complete lifecycle
                Result<List<AuditLog>> historyResult = userTemplateService.getTemplateAuditHistory(lifecycle.getId());
                assertTrue(historyResult.getCode() == 200);

                List<AuditLog> history = historyResult.getData();
                assertNotNull(history);

                // Should have at least 6 entries (initial creation, content save, submit,
                // reject, resubmit, approve)
                assertTrue(history.size() >= 6, "Should have at least 6 audit entries");

                // Verify the most recent entry is the approval
                assertEquals(UserTemplateStatus.REVIEW_APPROVED, history.get(0).getNewStatus(),
                                "Latest status should be REVIEW_APPROVED");
                assertEquals(adminUser.getId(), history.get(0).getOperatorId(),
                                "Last operation should be by admin");

                // Verify we can retrieve the template and its final status
                Optional<UserTemplate> result = userTemplateRepository.findById(lifecycle.getId());
                assertTrue(result.isPresent());
                assertEquals(UserTemplateStatus.REVIEW_APPROVED, result.get().getStatus(),
                                "Final status should be REVIEW_APPROVED");
        }

        @Test
        public void testMultipleTemplateConcurrentAuditing() {
                // Create two different templates
                UserTemplate template1 = new UserTemplate();
                template1.setUserId(normalUser.getId());
                template1.setTemplateId(testTemplate.getId());
                template1.setStatus(UserTemplateStatus.FILLING);
                template1.setRemarks("模板1初始数据");
                template1 = userTemplateRepository.save(template1);

                UserTemplate template2 = new UserTemplate();
                template2.setUserId(normalUser.getId());
                template2.setTemplateId(testTemplate.getId());
                template2.setStatus(UserTemplateStatus.FILLING);
                template2.setRemarks("模板2初始数据");
                template2 = userTemplateRepository.save(template2);

                // Perform operations on both templates interleaved (simulating concurrent
                // activity)

                // First operations on template1
                userTemplateService.updateTemplateStatus(
                                template1.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "模板1提交审核",
                                normalUser.getId(),
                                false);

                // First operations on template2
                userTemplateService.updateTemplateStatus(
                                template2.getId(),
                                UserTemplateStatus.UNDER_REVIEW,
                                "模板2提交审核",
                                normalUser.getId(),
                                false);

                // Second operations on template1
                userTemplateService.updateTemplateStatus(
                                template1.getId(),
                                UserTemplateStatus.REVIEW_APPROVED,
                                "模板1审核通过",
                                adminUser.getId(),
                                true);

                // Second operations on template2
                userTemplateService.updateTemplateStatus(
                                template2.getId(),
                                UserTemplateStatus.RETURNED,
                                "模板2需要修改",
                                adminUser.getId(),
                                true);

                // Verify audit histories are separate and correct
                Result<List<AuditLog>> history1Result = userTemplateService.getTemplateAuditHistory(template1.getId());
                Result<List<AuditLog>> history2Result = userTemplateService.getTemplateAuditHistory(template2.getId());

                List<AuditLog> history1 = history1Result.getData();
                List<AuditLog> history2 = history2Result.getData();

                // Check that each template has its own separate history
                for (AuditLog log : history1) {
                        assertEquals(template1.getId(), log.getEntityId(), "Log should belong to template1");
                }

                for (AuditLog log : history2) {
                        assertEquals(template2.getId(), log.getEntityId(), "Log should belong to template2");
                }

                // Verify final states
                Optional<UserTemplate> template1Result = userTemplateRepository.findById(template1.getId());
                Optional<UserTemplate> template2Result = userTemplateRepository.findById(template2.getId());

                assertEquals(UserTemplateStatus.REVIEW_APPROVED, template1Result.get().getStatus(),
                                "Template1 should be approved");
                assertEquals(UserTemplateStatus.RETURNED, template2Result.get().getStatus(),
                                "Template2 should be returned");
        }
}