package com.example.filing.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.filing.config.TestSecurityConfig;
import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.dto.request.ApplyTemplateRequest;
import com.example.filing.entity.AuditLog;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.service.UserTemplateService;
import com.example.filing.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 用户模板控制器测试类
 */
@WebMvcTest(controllers = UserTemplateController.class)
@Import(TestSecurityConfig.class)
class UserTemplateControllerTest {

        // 测试常量
        private static final String TEST_TEMPLATE_ID = "test-template-id";
        private static final String ADMIN_USER_ID = "admin-user-id";
        private static final String NORMAL_USER_ID = "normal-user-id";

        @Autowired
        private WebApplicationContext context;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserTemplateRepository userTemplateRepository;

        @MockBean
        private UserTemplateService userTemplateService;

        /**
         * 测试前的准备工作
         */
        @BeforeEach
        void setup() {
                // 重新配置MockMvc
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                // 设置模拟的审核历史数据
                setupMockAuditLogs();

                // 设置通用的服务层响应
                setupCommonServiceResponses();
        }

        /**
         * 初始化模拟的审核历史数据
         */
        private void setupMockAuditLogs() {
                var mockAuditLogs = List.of(
                                createAuditLog(
                                                "user_template",
                                                TEST_TEMPLATE_ID,
                                                "状态变更",
                                                UserTemplateStatus.FILLING,
                                                UserTemplateStatus.UNDER_REVIEW,
                                                NORMAL_USER_ID,
                                                "普通用户",
                                                false,
                                                "用户提交审核"),
                                createAuditLog(
                                                "user_template",
                                                TEST_TEMPLATE_ID,
                                                "状态变更",
                                                UserTemplateStatus.UNDER_REVIEW,
                                                UserTemplateStatus.REVIEW_APPROVED,
                                                ADMIN_USER_ID,
                                                "管理员",
                                                true,
                                                "管理员审核通过"));

                // 配置查询审核历史的服务返回值
                when(userTemplateService.getTemplateAuditHistory(anyString()))
                                .thenReturn(Result.success(mockAuditLogs));
        }

        /**
         * 初始化通用的服务层响应
         */
        private void setupCommonServiceResponses() {
                // 配置状态更新的服务返回值
                when(userTemplateService.updateTemplateStatus(
                                anyString(), anyInt(), anyString(), anyString(), anyBoolean()))
                                .thenReturn(Result.success("状态更新成功"));
        }

        /**
         * 创建审核日志对象
         */
        private AuditLog createAuditLog(
                        String entityType,
                        String entityId,
                        String operationType,
                        Integer oldStatus,
                        Integer newStatus,
                        String operatorId,
                        String operatorName,
                        boolean isAdmin,
                        String details) {

                var log = new AuditLog();
                log.setEntityType(entityType);
                log.setEntityId(entityId);
                log.setOperationType(operationType);
                log.setOldStatus(oldStatus);
                log.setNewStatus(newStatus);
                log.setOperatorId(operatorId);
                log.setOperatorName(operatorName);
                log.setAdmin(isAdmin);
                log.setDetails(details);
                return log;
        }

        /**
         * 创建用户模板对象
         */
        private UserTemplate createUserTemplate(String id, String userId, int status, String content) {
                var template = new UserTemplate();
                template.setId(id);
                template.setUserId(userId);
                template.setStatus(status);
                template.setContent(content);
                return template;
        }

        /**
         * 执行POST请求并返回结果
         */
        private ResultActions performPost(String url, String paramName, String paramValue,
                        String content, String username, String... roles) throws Exception {
                MockHttpServletRequestBuilder request = post(url)
                                .with(csrf())
                                .with(user(username).roles(roles))
                                .param(paramName, paramValue)
                                .contentType(MediaType.APPLICATION_JSON);

                if (content != null) {
                        request.content(content);
                }

                return mockMvc.perform(request).andDo(print());
        }

        /**
         * 执行GET请求并返回结果
         */
        private ResultActions performGet(String url, String paramName, String paramValue,
                        String username, String... roles) throws Exception {
                return mockMvc.perform(get(url)
                                .with(user(username).roles(roles))
                                .param(paramName, paramValue)
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print());
        }

        // 包含更多测试...

        /**
         * 提交审核功能测试
         */
        @Nested
        @DisplayName("提交审核测试")
        class SubmitForReviewTests {

                @Test
                @DisplayName("成功提交审核")
                @WithMockUser(username = "user", authorities = { "ROLE_USER" })
                void submitForReview_Success() throws Exception {
                        // 设置模拟数据 - 有效的填写中状态模板
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user",
                                                        UserTemplateStatus.FILLING,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 执行请求
                        performPost("/api/userTemplate/submitForReview", "id", TEST_TEMPLATE_ID, null, "user", "USER")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").value("已提交审核，请等待管理员审核"));
                }

                @Test
                @DisplayName("提交审核失败 - 无效状态")
                @WithMockUser(username = "user", authorities = { "ROLE_USER" })
                void submitForReview_InvalidStatus() throws Exception {
                        // 设置模拟数据 - 已经是审核中状态的模板
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user",
                                                        UserTemplateStatus.UNDER_REVIEW,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 执行请求
                        performPost("/api/userTemplate/submitForReview", "id", TEST_TEMPLATE_ID, null, "user", "USER")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("模板已是审核中，不能提交审核"));
                }

                @Test
                @DisplayName("提交审核失败 - 内容为空")
                @WithMockUser(username = "user", authorities = { "ROLE_USER" })
                void submitForReview_EmptyContent() throws Exception {
                        // 设置模拟数据 - 没有内容的模板
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user",
                                                        UserTemplateStatus.FILLING,
                                                        null)));

                        // 执行请求
                        performPost("/api/userTemplate/submitForReview", "id", TEST_TEMPLATE_ID, null, "user", "USER")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("模板内容不能为空，请先填写内容"));
                }

                @Test
                @DisplayName("提交审核失败 - 无权操作")
                @WithMockUser(username = "other-user", authorities = { "ROLE_USER" })
                void submitForReview_Unauthorized() throws Exception {
                        // 设置模拟数据 - 属于其他用户的模板
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user", // 不是当前认证用户
                                                        UserTemplateStatus.FILLING,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 执行请求
                        performPost("/api/userTemplate/submitForReview", "id", TEST_TEMPLATE_ID, null, "other-user",
                                        "USER")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("无权操作他人的模板"));
                }

                @Test
                @DisplayName("提交审核失败 - 模板不存在")
                @WithMockUser(username = "user", authorities = { "ROLE_USER" })
                void submitForReview_TemplateNotFound() throws Exception {
                        // 设置模拟数据 - 模板不存在
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.empty());

                        // 执行请求
                        performPost("/api/userTemplate/submitForReview", "id", TEST_TEMPLATE_ID, null, "user", "USER")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("模板不存在"));
                }
        }

        /**
         * 审核模板功能测试
         */
        @Nested
        @DisplayName("审核模板测试")
        class ReviewTemplateTests {

                @Test
                @DisplayName("管理员审核通过")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void reviewTemplate_Approve() throws Exception {
                        // 设置测试数据 - 模板处于审核中状态
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user",
                                                        UserTemplateStatus.UNDER_REVIEW,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 详细审核通过意见
                        String approvalComment = "审核通过，内容符合标准规范，批准备案";

                        // 执行请求
                        performPost("/api/userTemplate/reviewTemplate",
                                        "id", TEST_TEMPLATE_ID,
                                        approvalComment,
                                        "admin", "ADMIN")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").value("审核通过"));

                        // 验证服务调用参数
                        verify(userTemplateService).updateTemplateStatus(
                                        TEST_TEMPLATE_ID,
                                        UserTemplateStatus.REVIEW_APPROVED,
                                        approvalComment,
                                        "admin",
                                        true);
                }

                @Test
                @DisplayName("管理员退回修改")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void reviewTemplate_Return() throws Exception {
                        // 设置测试数据 - 模板处于审核中状态
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user",
                                                        UserTemplateStatus.UNDER_REVIEW,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 详细退回意见
                        String returnFeedback = "需要修改：1. 请补充项目预算明细；2. 完善风险评估部分；3. 提供更详细的实施计划";

                        // 执行请求
                        performPost("/api/userTemplate/reviewTemplate",
                                        "id", TEST_TEMPLATE_ID,
                                        returnFeedback,
                                        "admin", "ADMIN")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").value("已退回用户修改"));

                        // 验证服务调用参数
                        verify(userTemplateService).updateTemplateStatus(
                                        TEST_TEMPLATE_ID,
                                        UserTemplateStatus.RETURNED,
                                        returnFeedback,
                                        "admin",
                                        true);
                }

                @Test
                @DisplayName("审核失败 - 服务层错误")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void reviewTemplate_ServiceFailure() throws Exception {
                        // 设置服务层返回失败
                        when(userTemplateService.updateTemplateStatus(
                                        anyString(),
                                        eq(UserTemplateStatus.REVIEW_APPROVED),
                                        anyString(),
                                        anyString(),
                                        anyBoolean()))
                                        .thenReturn(Result.failed("审核失败：模板状态不允许变更"));

                        // 执行请求
                        performPost("/api/userTemplate/reviewTemplate",
                                        "id", TEST_TEMPLATE_ID,
                                        "尝试审核不在审核中状态的模板",
                                        "admin", "ADMIN")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("审核失败：模板状态不允许变更"));
                }

                @Test
                @DisplayName("审核失败 - 无效状态值")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void reviewTemplate_InvalidStatus() throws Exception {
                        // 执行请求 - 使用无效的状态值(不是审核通过或退回)
                        mockMvc.perform(post("/api/userTemplate/reviewTemplate")
                                        .with(csrf())
                                        .with(user("admin").roles("ADMIN"))
                                        .param("id", TEST_TEMPLATE_ID)
                                        .param("status", String.valueOf(UserTemplateStatus.FILLING)) // 无效状态
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("Invalid status test"))
                                        .andDo(print())
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("无效的审核状态，只能是审核通过(6)或退回(7)"));
                }

                @Test
                @DisplayName("审核失败 - 无权限(普通用户)")
                @WithMockUser(username = "user", authorities = { "ROLE_USER" })
                void reviewTemplate_Forbidden() throws Exception {
                        // 执行请求 - 普通用户尝试审核
                        mockMvc.perform(post("/api/userTemplate/reviewTemplate")
                                        .with(csrf())
                                        .param("id", TEST_TEMPLATE_ID)
                                        .param("status", String.valueOf(UserTemplateStatus.REVIEW_APPROVED))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("审核通过"))
                                        .andExpect(status().isForbidden());
                }
        }

        /**
         * 审核历史查询功能测试
         */
        @Nested
        @DisplayName("审核历史查询测试")
        class AuditHistoryTests {

                @Test
                @DisplayName("管理员查询审核历史")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void getAuditHistory_AsAdmin() throws Exception {
                        // 执行请求
                        performGet("/api/userTemplate/getAuditHistory", "id", TEST_TEMPLATE_ID, "admin", "ADMIN")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").isArray())
                                        .andExpect(jsonPath("$.data.length()").value(2));
                }

                @Test
                @DisplayName("用户查询自己的审核历史")
                @WithMockUser(username = "user", authorities = { "ROLE_USER" })
                void getAuditHistory_AsUser() throws Exception {
                        // 设置测试数据 - 用户自己的模板
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "user",
                                                        UserTemplateStatus.REVIEW_APPROVED,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 执行请求
                        performGet("/api/userTemplate/getAuditHistory", "id", TEST_TEMPLATE_ID, "user", "USER")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").isArray())
                                        .andExpect(jsonPath("$.data.length()").value(2));
                }

                @Test
                @DisplayName("用户查询他人审核历史被拒绝")
                @WithMockUser(username = "unauthorized-user", authorities = { "ROLE_USER" })
                void getAuditHistory_Unauthorized() throws Exception {
                        // 设置测试数据 - 其他用户的模板
                        when(userTemplateRepository.findById(TEST_TEMPLATE_ID))
                                        .thenReturn(Optional.of(createUserTemplate(
                                                        TEST_TEMPLATE_ID,
                                                        "other-user", // 不是请求用户
                                                        UserTemplateStatus.REVIEW_APPROVED,
                                                        "{\"data\": {\"field1\": \"value1\"}}")));

                        // 执行请求
                        performGet("/api/userTemplate/getAuditHistory", "id", TEST_TEMPLATE_ID, "unauthorized-user",
                                        "USER")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("无权查看他人的模板审核历史"));
                }

                @Test
                @DisplayName("查询带过滤条件的审核历史")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void getAuditHistory_WithFilters() throws Exception {
                        // 执行请求 - 带操作类型过滤条件
                        mockMvc.perform(get("/api/userTemplate/getAuditHistory")
                                        .with(user("admin").roles("ADMIN"))
                                        .param("id", TEST_TEMPLATE_ID)
                                        .param("operationType", "状态变更") // 过滤条件
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").isArray());
                }

                @Test
                @DisplayName("查询不存在的模板审核历史")
                @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
                void getAuditHistory_TemplateNotFound() throws Exception {
                        // 设置服务层返回失败
                        when(userTemplateService.getTemplateAuditHistory("non-existent-id"))
                                        .thenReturn(Result.failed("用户模板关系不存在"));

                        // 执行请求
                        performGet("/api/userTemplate/getAuditHistory", "id", "non-existent-id", "admin", "ADMIN")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("用户模板关系不存在"));
                }
        }

        /**
         * 模板申请功能测试
         */
        @Nested
        @DisplayName("模板申请测试")
        class ApplyTemplateTests {

                @Test
                @DisplayName("普通用户申请模板")
                void applyTemplate_AsUser() throws Exception {
                        // 准备测试数据
                        var request = new ApplyTemplateRequest();
                        request.setUserIds(List.of("user"));

                        var relationIds = List.of("relation-1");
                        var serviceResult = Result.success("模板申请成功", relationIds);

                        // 模拟服务响应
                        when(userTemplateService.applyTemplate(
                                        eq("template-123"),
                                        anyList(),
                                        eq("user"),
                                        eq(false)))
                                        .thenReturn(serviceResult);

                        // 执行请求和验证
                        mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                        .with(csrf())
                                        .with(user("user").roles("USER"))
                                        .param("templateId", "template-123")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("模板申请成功"))
                                        .andExpect(jsonPath("$.data[0]").value("relation-1"));
                }

                @Test
                @DisplayName("管理员发送模板")
                void applyTemplate_AsAdmin() throws Exception {
                        // 准备测试数据
                        var request = new ApplyTemplateRequest();
                        request.setUserIds(List.of("user1", "user2"));

                        var relationIds = List.of("relation-1", "relation-2");
                        var serviceResult = Result.success("模板发送成功", relationIds);

                        // 模拟服务响应
                        when(userTemplateService.applyTemplate(
                                        eq("template-123"),
                                        anyList(),
                                        eq("admin"),
                                        eq(true)))
                                        .thenReturn(serviceResult);

                        // 执行请求和验证
                        mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                        .with(csrf())
                                        .with(user("admin").roles("ADMIN"))
                                        .param("templateId", "template-123")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("模板发送成功"))
                                        .andExpect(jsonPath("$.data.length()").value(2));
                }

                @Test
                @DisplayName("申请失败 - 空用户列表")
                void applyTemplate_EmptyUserIds() throws Exception {
                        // 准备测试数据
                        var request = new ApplyTemplateRequest();
                        request.setUserIds(List.of());

                        // 执行请求和验证
                        mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                        .with(csrf())
                                        .with(user("user").roles("USER"))
                                        .param("templateId", "template-123")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andDo(print())
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("用户ID列表不能为空"));
                }

                @Test
                @DisplayName("申请失败 - 模板不存在")
                void applyTemplate_TemplateNotFound() throws Exception {
                        // 准备测试数据
                        var request = new ApplyTemplateRequest();
                        request.setUserIds(List.of("user"));

                        // 模拟服务响应
                        when(userTemplateService.applyTemplate(
                                        anyString(),
                                        anyList(),
                                        anyString(),
                                        anyBoolean()))
                                        .thenReturn(Result.failed("模板不存在"));

                        // 执行请求和验证
                        mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                        .with(csrf())
                                        .with(user("user").roles("USER"))
                                        .param("templateId", "non-existent-template")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andDo(print())
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("模板不存在"));
                }
        }

        /**
         * 模板内容管理测试
         */
        @Nested
        @DisplayName("模板内容管理测试")
        class TemplateContentTests {

                @Test
                @DisplayName("获取模板内容")
                void getTemplateContent_Success() throws Exception {
                        // 准备测试数据
                        String userTemplateId = "template-relation-123";
                        String contentJson = "{\"formData\":{\"field1\":\"value1\"}}";

                        // 模拟服务响应
                        when(userTemplateService.getTemplateContent(eq(userTemplateId)))
                                        .thenReturn(Result.success(contentJson));

                        // 执行请求和验证
                        performGet("/api/userTemplate/getTemplateContent", "id", userTemplateId, "user", "USER")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data").value(contentJson));
                }

                @Test
                @DisplayName("获取模板内容失败 - 模板不存在")
                void getTemplateContent_NotFound() throws Exception {
                        // 准备测试数据
                        String userTemplateId = "non-existent-relation";

                        // 模拟服务响应
                        when(userTemplateService.getTemplateContent(anyString()))
                                        .thenReturn(Result.failed("用户模板关系不存在"));

                        // 执行请求和验证
                        performGet("/api/userTemplate/getTemplateContent", "id", userTemplateId, "user", "USER")
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(500))
                                        .andExpect(jsonPath("$.message").value("用户模板关系不存在"));
                }

                @Test
                @DisplayName("保存模板内容")
                void saveTemplateContent_Success() throws Exception {
                        // 准备测试数据
                        String userTemplateId = "template-relation-123";
                        String contentToSave = "{\"formData\":{\"field1\":\"new-value\"}}";

                        // 模拟服务响应
                        when(userTemplateService.saveTemplateContent(
                                        eq(userTemplateId),
                                        anyString(),
                                        eq("user")))
                                        .thenReturn(Result.success("内容保存成功"));

                        // 执行请求和验证
                        performPost("/api/userTemplate/saveTemplateContent", "id", userTemplateId, contentToSave,
                                        "user", "USER")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("内容保存成功"));
                }
        }

        /**
         * 状态更新测试
         */
        @Nested
        @DisplayName("状态更新测试")
        class UpdateStatusTests {

                @Test
                @DisplayName("用户更新状态成功")
                void updateStatus_AsUser_Success() throws Exception {
                        // 准备测试数据 - 普通用户将状态从填写中(4)变更为审核中(5)
                        String userTemplateId = "template-relation-123";
                        String remarks = "提交审核";

                        // 模拟服务响应
                        when(userTemplateService.updateTemplateStatus(
                                        eq(userTemplateId),
                                        eq(UserTemplateStatus.UNDER_REVIEW),
                                        eq(remarks),
                                        eq("user"),
                                        eq(false)))
                                        .thenReturn(Result.success("状态更新成功"));

                        // 执行请求和验证
                        performPost("/api/userTemplate/updateTemplateStatus",
                                        "id", userTemplateId,
                                        remarks,
                                        "user", "USER")
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("状态更新成功"));
                }

                @Test
                @DisplayName("管理员更新状态成功")
                void updateStatus_AsAdmin_Success() throws Exception {
                        // 准备测试数据 - 管理员将状态从审核中(5)变更为审核通过(6)
                        String userTemplateId = "template-relation-123";
                        String remarks = "审核通过";

                        // 模拟服务响应
                        when(userTemplateService.updateTemplateStatus(
                                        eq(userTemplateId),
                                        eq(UserTemplateStatus.REVIEW_APPROVED),
                                        eq(remarks),
                                        eq("admin"),
                                        eq(true)))
                                        .thenReturn(Result.success("状态更新成功"));

                        // 执行请求和验证
                        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                                        .with(csrf())
                                        .with(user("admin").roles("ADMIN"))
                                        .param("id", userTemplateId)
                                        .param("status", String.valueOf(UserTemplateStatus.REVIEW_APPROVED))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(remarks))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("状态更新成功"));
                }
        }
}