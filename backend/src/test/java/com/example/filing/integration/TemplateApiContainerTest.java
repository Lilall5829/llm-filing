package com.example.filing.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.filing.config.TestContainersConfig;
import com.example.filing.config.TestPasswordConfig;
import com.example.filing.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 使用TestContainers进行模板API测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Import({ TestContainersConfig.class, TestPasswordConfig.class })
public class TemplateApiContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("测试获取模板列表")
    public void testGetTemplateList() throws Exception {
        // 获取管理员Token
        String adminToken = TestUtil.getAdminToken(mockMvc, objectMapper);

        // 测试获取模板列表
        mockMvc.perform(get("/api/templateRegistry/page")
                .header("Authorization", "Bearer " + adminToken)
                .param("current", "1")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").isNumber());
    }

    @Test
    @DisplayName("测试获取模板详情")
    public void testGetTemplateById() throws Exception {
        // 获取管理员Token
        String adminToken = TestUtil.getAdminToken(mockMvc, objectMapper);

        // 测试获取模板详情
        mockMvc.perform(get("/api/templateRegistry/getTemplateRegistryById")
                .header("Authorization", "Bearer " + adminToken)
                .param("id", "template-id"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.templateCode").exists())
                .andExpect(jsonPath("$.data.templateName").exists());
    }
}