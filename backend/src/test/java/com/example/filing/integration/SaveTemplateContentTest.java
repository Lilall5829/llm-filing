package com.example.filing.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

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
import com.example.filing.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 用于测试saveTemplateContent方法的专用测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Import({ TestContainersConfig.class, TestPasswordConfig.class })
public class SaveTemplateContentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("测试使用字符串格式保存模板内容")
    public void testSaveTemplateContentUsingString() throws Exception {
        // 获取用户Token
        String userToken = TestUtil.getUserToken(mockMvc, objectMapper);

        // 使用简单字符串作为内容
        String content = "测试内容";

        // 发送请求
        mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .contentType(MediaType.TEXT_PLAIN)
                .content(content))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("测试使用JSON字符串格式保存模板内容")
    public void testSaveTemplateContentUsingJsonString() throws Exception {
        // 获取用户Token
        String userToken = TestUtil.getUserToken(mockMvc, objectMapper);

        // 使用JSON字符串作为内容
        String content = "{\"field1\":\"测试内容\"}";

        // 发送请求
        mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("测试使用Map转换为JSON保存模板内容")
    public void testSaveTemplateContentUsingMap() throws Exception {
        // 获取用户Token
        String userToken = TestUtil.getUserToken(mockMvc, objectMapper);

        // 使用Map创建内容，然后转为JSON
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "测试内容");
        String jsonContent = objectMapper.writeValueAsString(content);

        // 发送请求
        mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}