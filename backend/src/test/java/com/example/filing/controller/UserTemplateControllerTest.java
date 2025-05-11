package com.example.filing.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.filing.config.TestSecurityConfig;
import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.dto.request.ApplyTemplateRequest;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.service.UserTemplateService;
import com.example.filing.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserTemplateController.class)
@Import(TestSecurityConfig.class)
class UserTemplateControllerTest {

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

        @BeforeEach
        public void setup() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        void testApplyTemplate_AsUser_Success() throws Exception {
                // 准备测试数据
                ApplyTemplateRequest request = new ApplyTemplateRequest();
                request.setUserIds(Arrays.asList("user"));

                List<String> relationIds = Arrays.asList("relation-1");
                Result<List<String>> serviceResult = Result.success("模板申请成功", relationIds);

                // 模拟服务响应
                when(userTemplateService.applyTemplate(
                                eq("template-123"),
                                anyList(),
                                eq("user"),
                                eq(false)))
                                .thenReturn(serviceResult);

                // 执行请求和验证 - 使用 user() 显式传递认证信息
                mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                .with(csrf())
                                .with(user("user").roles("USER")) // 明确提供用户认证信息
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
        void testApplyTemplate_AsAdmin_Success() throws Exception {
                // 准备测试数据
                ApplyTemplateRequest request = new ApplyTemplateRequest();
                request.setUserIds(Arrays.asList("user1", "user2"));

                List<String> relationIds = Arrays.asList("relation-1", "relation-2");
                Result<List<String>> serviceResult = Result.success("模板发送成功", relationIds);

                // 模拟服务响应
                when(userTemplateService.applyTemplate(
                                eq("template-123"),
                                anyList(),
                                eq("admin"),
                                eq(true)))
                                .thenReturn(serviceResult);

                // 执行请求和验证 - 使用 user() 显式传递认证信息
                mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                .with(csrf())
                                .with(user("admin").roles("ADMIN")) // 明确提供管理员认证信息
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
        void testApplyTemplate_EmptyUserIds() throws Exception {
                // 准备测试数据
                ApplyTemplateRequest request = new ApplyTemplateRequest();
                request.setUserIds(Arrays.asList());

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                .with(csrf())
                                .with(user("user").roles("USER")) // 添加用户认证信息
                                .param("templateId", "template-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("用户ID列表不能为空"));
        }

        @Test
        void testApplyTemplate_FailureResponse() throws Exception {
                // 准备测试数据
                ApplyTemplateRequest request = new ApplyTemplateRequest();
                request.setUserIds(Arrays.asList("user"));

                Result<List<String>> serviceResult = Result.failed("模板不存在");

                // 模拟服务响应
                when(userTemplateService.applyTemplate(
                                anyString(),
                                anyList(),
                                anyString(),
                                anyBoolean()))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/applyTemplate")
                                .with(csrf())
                                .with(user("user").roles("USER")) // 添加用户认证信息
                                .param("templateId", "non-existent-template")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("模板不存在"));
        }

        @Test
        void testGetTemplateContent_Success() throws Exception {
                // 准备测试数据
                String userTemplateId = "template-relation-123";
                String contentJson = "{\"formData\":{\"field1\":\"value1\"}}";
                Result<String> serviceResult = Result.success(contentJson);

                // 模拟服务响应
                when(userTemplateService.getTemplateContent(eq(userTemplateId)))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(get("/api/userTemplate/getTemplateContent")
                                .with(user("user").roles("USER"))
                                .param("id", userTemplateId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").value(contentJson));
        }

        @Test
        void testGetTemplateContent_NotFound() throws Exception {
                // 准备测试数据
                String userTemplateId = "non-existent-relation";
                Result<String> serviceResult = Result.failed("用户模板关系不存在");

                // 模拟服务响应
                when(userTemplateService.getTemplateContent(anyString()))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(get("/api/userTemplate/getTemplateContent")
                                .with(user("user").roles("USER"))
                                .param("id", userTemplateId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("用户模板关系不存在"));
        }

        @Test
        void testSaveTemplateContent_Success() throws Exception {
                // 准备测试数据
                String userTemplateId = "template-relation-123";
                String contentToSave = "{\"formData\":{\"field1\":\"new-value\"}}";
                Result<String> serviceResult = Result.success("内容保存成功");

                // 模拟服务响应
                when(userTemplateService.saveTemplateContent(
                                eq(userTemplateId),
                                anyString(),
                                eq("user")))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                                .with(csrf())
                                .with(user("user").roles("USER"))
                                .param("id", userTemplateId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contentToSave))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("内容保存成功"));
        }

        @Test
        void testSaveTemplateContent_Unauthorized() throws Exception {
                // 准备测试数据
                String userTemplateId = "template-relation-123";
                String contentToSave = "{\"formData\":{\"field1\":\"new-value\"}}";
                Result<String> serviceResult = Result.failed("无权操作他人的模板");

                // 模拟服务响应
                when(userTemplateService.saveTemplateContent(
                                anyString(),
                                anyString(),
                                anyString()))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                                .with(csrf())
                                .with(user("unauthorized-user").roles("USER"))
                                .param("id", userTemplateId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contentToSave))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("无权操作他人的模板"));
        }

        @Test
        void testUpdateTemplateStatus_AsUser_Success() throws Exception {
                // 准备测试数据 - 用户将状态从填写中(4)变更为审核中(5)
                String userTemplateId = "template-relation-123";
                String remarks = "提交审核";
                Result<String> serviceResult = Result.success("状态更新成功");

                // 模拟服务响应
                when(userTemplateService.updateTemplateStatus(
                                eq(userTemplateId),
                                eq(UserTemplateStatus.UNDER_REVIEW),
                                eq(remarks),
                                eq("user"),
                                eq(false)))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                                .with(csrf())
                                .with(user("user").roles("USER"))
                                .param("id", userTemplateId)
                                .param("status", String.valueOf(UserTemplateStatus.UNDER_REVIEW))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(remarks))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("状态更新成功"));
        }

        @Test
        void testUpdateTemplateStatus_AsUser_Unauthorized() throws Exception {
                // 准备测试数据 - 用户尝试将状态从审核中(5)变更为审核通过(6)，这不被允许
                String userTemplateId = "template-relation-123";
                String remarks = "尝试审核通过";
                Result<String> serviceResult = Result.failed("当前状态不允许变更为目标状态");

                // 模拟服务响应
                when(userTemplateService.updateTemplateStatus(
                                anyString(),
                                anyInt(),
                                anyString(),
                                anyString(),
                                eq(false)))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                                .with(csrf())
                                .with(user("user").roles("USER"))
                                .param("id", userTemplateId)
                                .param("status", String.valueOf(UserTemplateStatus.REVIEW_APPROVED))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(remarks))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("当前状态不允许变更为目标状态"));
        }

        @Test
        void testUpdateTemplateStatus_AsAdmin_Success() throws Exception {
                // 准备测试数据 - 管理员将状态从审核中(5)变更为审核通过(6)
                String userTemplateId = "template-relation-123";
                String remarks = "审核通过";
                Result<String> serviceResult = Result.success("状态更新成功");

                // 模拟服务响应
                when(userTemplateService.updateTemplateStatus(
                                eq(userTemplateId),
                                eq(UserTemplateStatus.REVIEW_APPROVED),
                                eq(remarks),
                                eq("admin"),
                                eq(true)))
                                .thenReturn(serviceResult);

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

        @Test
        void testUpdateTemplateStatus_TemplateNotFound() throws Exception {
                // 准备测试数据 - 模板不存在
                String nonExistentId = "non-existent-id";
                String remarks = "状态更新";
                Result<String> serviceResult = Result.failed("用户模板关系不存在");

                // 模拟服务响应
                when(userTemplateService.updateTemplateStatus(
                                eq(nonExistentId),
                                anyInt(),
                                anyString(),
                                anyString(),
                                anyBoolean()))
                                .thenReturn(serviceResult);

                // 执行请求和验证
                mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                                .with(csrf())
                                .with(user("user").roles("USER"))
                                .param("id", nonExistentId)
                                .param("status", String.valueOf(UserTemplateStatus.UNDER_REVIEW))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(remarks))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("用户模板关系不存在"));
        }
}