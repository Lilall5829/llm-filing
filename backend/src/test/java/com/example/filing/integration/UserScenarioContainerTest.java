package com.example.filing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.filing.config.TestContainersConfig;
import com.example.filing.config.TestPasswordConfig;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 使用TestContainers进行用户场景测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Import({ TestContainersConfig.class, TestPasswordConfig.class })
public class UserScenarioContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserTemplateRepository userTemplateRepository;

    @Test
    @DisplayName("测试完整的用户模板工作流")
    public void testCompleteUserTemplateWorkflow() throws Exception {
        // 1. 用户登录并获取模板内容
        String userToken = TestUtil.getUserToken(mockMvc, objectMapper);

        // 2. 用户填写模板内容
        fillTemplateContent(userToken, "user-template-id");

        // 3. 用户提交审核
        submitForReview(userToken, "user-template-id");

        // 4. 管理员审核通过
        String adminToken = TestUtil.getAdminToken(mockMvc, objectMapper);
        approveTemplate(adminToken, "user-template-id");

        // 5. 验证最终状态
        Optional<UserTemplate> template = userTemplateRepository.findById("user-template-id");
        assertTrue(template.isPresent(), "用户模板应该存在");
        assertEquals(6, template.get().getStatus(), "状态应该是审核通过(6)");
    }

    private void fillTemplateContent(String userToken, String userTemplateId) throws Exception {
        // 创建一个合法的JSON内容字符串
        // 注意：content应该作为字符串传递给后端，后端会将其解析为JSON
        String content = "{\"field1\":\"测试内容\"}";

        mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", userTemplateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andDo(print()) // 添加输出以便调试
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void submitForReview(String userToken, String userTemplateId) throws Exception {
        // 直接使用/submitForReview端点更清晰，并符合控制器方法的要求
        mockMvc.perform(post("/api/userTemplate/submitForReview")
                .header("Authorization", "Bearer " + userToken)
                .param("id", userTemplateId)) // id作为参数传递，submitForReview不需要请求体
                .andDo(print()) // 添加输出以便调试
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void approveTemplate(String adminToken, String userTemplateId) throws Exception {
        // 修改为使用正确的参数传递方式
        String remarks = "审核通过";

        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .header("Authorization", "Bearer " + adminToken)
                .param("id", userTemplateId)
                .param("status", "6") // 6表示审核通过
                .contentType(MediaType.TEXT_PLAIN)
                .content(remarks)) // 将remarks作为请求体传递
                .andDo(print()) // 添加输出以便调试
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}