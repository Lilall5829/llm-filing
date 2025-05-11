package com.example.filing.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.filing.dto.AuthenticationRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 测试辅助工具类
 */
public final class TestUtil {

    // 使用Java 17的Record类优化认证请求和响应
    private record AuthRequest(String username, String password, String endpoint) {
    }

    // 注意:这里的密码必须与data-test.sql中使用的密码哈希对应
    private static final AuthRequest ADMIN_AUTH = new AuthRequest("admin", "admin123", "/api/auth/admin/login");
    private static final AuthRequest USER_AUTH = new AuthRequest("testuser", "password", "/api/auth/login");

    // 私有构造方法防止实例化
    private TestUtil() {
        throw new UnsupportedOperationException("工具类不应被实例化");
    }

    /**
     * 获取管理员Token
     */
    public static String getAdminToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return getToken(mockMvc, objectMapper, ADMIN_AUTH);
    }

    /**
     * 获取普通用户Token
     */
    public static String getUserToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return getToken(mockMvc, objectMapper, USER_AUTH);
    }

    /**
     * 获取Token通用方法
     */
    private static String getToken(MockMvc mockMvc, ObjectMapper objectMapper, AuthRequest authRequest)
            throws Exception {
        var request = new AuthenticationRequest();
        request.setLoginName(authRequest.username());
        request.setPassword(authRequest.password());

        MvcResult result = mockMvc.perform(post(authRequest.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        // 使用Java 17的类型推断和var关键字
        var responseMap = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
        });
        var data = (Map<String, Object>) responseMap.get("data");

        return (String) data.get("token");
    }

    /**
     * 保存模板内容的工作方法
     */
    public static MvcResult saveTemplateContent(MockMvc mockMvc, String token, String templateId, String content)
            throws Exception {
        // 创建简单的JSON内容
        String jsonContent = "\"" + content + "\""; // 注意，这里将内容转换为JSON字符串格式

        MvcResult result = mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + token)
                .param("id", templateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andDo(print())
                .andReturn();

        return result;
    }
}