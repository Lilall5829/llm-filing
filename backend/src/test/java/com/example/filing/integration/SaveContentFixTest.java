package com.example.filing.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.filing.config.TestContainersConfig;
import com.example.filing.config.TestPasswordConfig;
import com.example.filing.entity.SysUser;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 用于修复SaveTemplateContent问题的测试类
 * 使用多种不同的方式尝试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Import({ TestContainersConfig.class, TestPasswordConfig.class })
public class SaveContentFixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserTemplateRepository userTemplateRepository;

    @Autowired
    private TemplateRegistryRepository templateRegistryRepository;

    @Autowired
    private SysUserRepository sysUserRepository;

    private String userToken;
    private String adminToken;

    @BeforeEach
    public void setup() throws Exception {
        // 获取用户和管理员Token
        userToken = TestUtil.getUserToken(mockMvc, objectMapper);
        adminToken = TestUtil.getAdminToken(mockMvc, objectMapper);

        // 确保用户模板存在
        ensureUserTemplateExists();
    }

    /**
     * 确保测试用的UserTemplate存在
     */
    private void ensureUserTemplateExists() {
        // 获取testuser的ID - 这里需要获取正确的用户ID
        SysUser testUser = sysUserRepository.findByLoginName("testuser");

        if (testUser == null) {
            throw new RuntimeException("测试用户 'testuser' 不存在，请确保数据库中有此用户");
        }

        String userId = testUser.getId();
        System.out.println("测试用户真实ID: " + userId);

        // 检查测试模板是否存在
        if (!userTemplateRepository.existsById("user-template-id")) {
            UserTemplate userTemplate = new UserTemplate();
            userTemplate.setId("user-template-id");
            userTemplate.setUserId(userId); // 使用正确的用户ID
            userTemplate.setTemplateId("template-id");
            userTemplate.setStatus(4); // 填写中
            userTemplateRepository.save(userTemplate);

            System.out.println("创建了测试用户模板：" + userTemplate.getId() + "，用户ID：" + userId);
        } else {
            // 更新现有模板以确保userId正确
            UserTemplate userTemplate = userTemplateRepository.findById("user-template-id").orElse(null);
            if (userTemplate != null && !userTemplate.getUserId().equals(userId)) {
                userTemplate.setUserId(userId);
                userTemplateRepository.save(userTemplate);
                System.out.println("更新了测试用户模板的用户ID：" + userId + " (原ID: " + userTemplate.getUserId() + ")");
            }
        }
    }

    @Test
    @DisplayName("测试直接使用表单参数方式提交内容")
    public void testSaveTemplateContentWithFormParam() throws Exception {
        mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .param("content", "{\"field1\":\"测试内容\"}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试使用RAW方式提交内容")
    public void testSaveTemplateContentWithRawBody() throws Exception {
        // 首先打印用户ID信息以确认权限问题
        SysUser testUser = sysUserRepository.findByLoginName("testuser");
        System.out.println("测试用户ID: " + (testUser != null ? testUser.getId() : "null"));

        // 打印对应的用户模板
        UserTemplate userTemplate = userTemplateRepository.findById("user-template-id").orElse(null);
        System.out.println("用户模板信息: " + (userTemplate != null ? "ID=" + userTemplate.getId() +
                ", UserID=" + userTemplate.getUserId() +
                ", Status=" + userTemplate.getStatus() : "null"));

        // 尝试使用不同的JSON内容格式
        String content = "{\"field1\":\"测试内容\"}";

        // 创建请求构建器，确保设置了正确的内容类型
        MockHttpServletRequestBuilder requestBuilder = post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8");

        // 执行请求并捕获完整响应
        MvcResult result = mockMvc.perform(requestBuilder)
                .andDo(print())
                .andReturn();

        // 打印完整响应信息
        System.out.println("响应状态: " + result.getResponse().getStatus());
        System.out.println("响应内容: " + result.getResponse().getContentAsString());

        // 如果测试失败，我们仍然可以看到响应详情
        if (result.getResponse().getStatus() != 200) {
            System.out.println("测试失败 - 详细信息：");
            System.out.println("错误消息: " + result.getResponse().getContentAsString());
        } else {
            System.out.println("测试成功！");
        }

        // 现在使用断言验证状态
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试检查用户模板是否存在")
    public void testCheckUserTemplateExists() throws Exception {
        // 打印所有用户模板
        userTemplateRepository.findAll().forEach(ut -> {
            System.out.println("UserTemplate ID: " + ut.getId() + ", UserId: " + ut.getUserId());
        });

        // 打印正在使用的测试用户信息
        SysUser testUser = sysUserRepository.findByLoginName("testuser");
        if (testUser != null) {
            System.out.println("Test User ID: " + testUser.getId() + ", Name: " + testUser.getUserName());
        }

        // 检查特定ID的模板
        mockMvc.perform(get("/api/userTemplate/getTemplateContent")
                .header("Authorization", "Bearer " + adminToken)
                .param("id", "user-template-id"))
                .andDo(print());
    }

    @Test
    @DisplayName("测试审核模板状态")
    public void testUpdateTemplateStatus() throws Exception {
        // 尝试更新状态，这应该可以正常工作
        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .header("Authorization", "Bearer " + adminToken)
                .param("id", "user-template-id")
                .param("status", "5") // 审核中
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"状态更新测试\""))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试通过URL传递内容")
    public void testSaveTemplateContentWithUrlEncoded() throws Exception {
        String content = java.net.URLEncoder.encode("{\"field1\":\"test content\"}", "UTF-8");

        mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .param("content", content))
                .andDo(print());
    }

    @Test
    @DisplayName("测试诊断服务器端处理方式")
    public void testDiagnoseServerProcessing() throws Exception {
        // 尝试更新状态，这应该可以正常工作
        MvcResult result = mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .header("Authorization", "Bearer " + adminToken)
                .param("id", "user-template-id")
                .param("status", "5") // 审核中
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"状态更新测试\""))
                .andReturn();

        System.out.println("状态更新响应: " + result.getResponse().getContentAsString());

        // 然后尝试保存内容
        MvcResult contentResult = mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", "user-template-id")
                .contentType(MediaType.TEXT_PLAIN)
                .content("测试内容"))
                .andReturn();

        System.out.println("内容保存响应: " + contentResult.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("测试JWT Token与用户ID匹配问题")
    public void testJwtTokenUserIdProblem() throws Exception {
        // 先获取测试用户信息
        SysUser testUser = sysUserRepository.findByLoginName("testuser");
        if (testUser == null) {
            throw new RuntimeException("测试用户 'testuser' 不存在，请确保数据库中有此用户");
        }

        String userId = testUser.getId();
        System.out.println("JWT测试 - 测试用户真实ID: " + userId);

        // 打印用户名（这是JWT token中的主题/subject）
        System.out.println("JWT测试 - 测试用户登录名: " + testUser.getLoginName());

        // 从用户Token中解析出用户名
        String tokenSubject = extractSubjectFromToken(userToken);
        System.out.println("JWT测试 - Token中的subject: " + tokenSubject);

        // 创建一个新的测试模板，确保使用正确的用户ID
        String testTemplateId = "jwt-test-template-id";
        UserTemplate userTemplate = new UserTemplate();
        userTemplate.setId(testTemplateId);
        userTemplate.setUserId(userId);
        userTemplate.setTemplateId("template-id");
        userTemplate.setStatus(4); // 填写中
        userTemplateRepository.save(userTemplate);
        System.out.println("JWT测试 - 创建了测试模板: " + testTemplateId + ", 用户ID: " + userId);

        // 使用新创建的模板ID进行测试
        String content = "{\"field1\":\"JWT测试内容\"}";

        MockHttpServletRequestBuilder requestBuilder = post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", testTemplateId)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8");

        MvcResult result = mockMvc.perform(requestBuilder)
                .andDo(print())
                .andReturn();

        System.out.println("JWT测试 - 响应状态: " + result.getResponse().getStatus());
        System.out.println("JWT测试 - 响应内容: " + result.getResponse().getContentAsString());
    }

    /**
     * 从JWT Token中提取Subject (用户名)
     */
    private String extractSubjectFromToken(String token) {
        // 简单实现，直接解析base64部分
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                return null;
            }

            String payload = new String(java.util.Base64.getDecoder().decode(chunks[1]));
            System.out.println("JWT测试 - Token payload: " + payload);

            // 使用简单字符串搜索找到主题
            int subStart = payload.indexOf("\"sub\":\"") + 7;
            int subEnd = payload.indexOf("\"", subStart);

            return payload.substring(subStart, subEnd);
        } catch (Exception e) {
            System.out.println("JWT测试 - 解析Token异常: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("修复模板内容保存问题")
    public void testFixContentSaveProblem() throws Exception {
        // 获取测试用户ID
        SysUser testUser = sysUserRepository.findByLoginName("testuser");
        if (testUser == null) {
            throw new RuntimeException("测试用户 'testuser' 不存在，请确保数据库中有此用户");
        }

        String userId = testUser.getId();
        System.out.println("修复测试 - 用户ID: " + userId);

        // 创建一个新的测试模板，确保使用正确的用户ID
        String testTemplateId = "fix-test-template-id";

        // 首先检查模板是否已存在，如果存在则删除
        if (userTemplateRepository.existsById(testTemplateId)) {
            userTemplateRepository.deleteById(testTemplateId);
            System.out.println("修复测试 - 删除了已存在的模板: " + testTemplateId);
        }

        // 创建新的测试模板
        UserTemplate userTemplate = new UserTemplate();
        userTemplate.setId(testTemplateId);
        userTemplate.setUserId(userId);
        userTemplate.setTemplateId("template-id");
        userTemplate.setStatus(4); // 填写中
        userTemplate = userTemplateRepository.save(userTemplate);
        System.out.println("修复测试 - 创建了测试模板: " + testTemplateId + ", 用户ID: " + userTemplate.getUserId());

        // 确认模板已正确保存
        UserTemplate savedTemplate = userTemplateRepository.findById(testTemplateId).orElse(null);
        if (savedTemplate == null) {
            throw new RuntimeException("无法读取新创建的测试模板");
        }
        System.out.println("修复测试 - 确认模板存在，用户ID: " + savedTemplate.getUserId());

        // 创建最简单的内容
        String simpleContent = "简单文本内容";

        // 尝试直接使用表单方式提交
        MvcResult formResult = mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", testTemplateId)
                .param("content", simpleContent)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(print())
                .andReturn();

        System.out.println("修复测试 - 表单提交响应: " + formResult.getResponse().getStatus()
                + " - " + formResult.getResponse().getContentAsString());

        // 也尝试使用JSON方式提交
        String jsonContent = "{\"test\": \"JSON内容\"}";

        MvcResult jsonResult = mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + userToken)
                .param("id", testTemplateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andDo(print())
                .andReturn();

        System.out.println("修复测试 - JSON提交响应: " + jsonResult.getResponse().getStatus()
                + " - " + jsonResult.getResponse().getContentAsString());

        // 尝试使用管理员权限操作
        MvcResult adminResult = mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                .header("Authorization", "Bearer " + adminToken)
                .param("id", testTemplateId)
                .contentType(MediaType.TEXT_PLAIN)
                .content("管理员测试内容"))
                .andDo(print())
                .andReturn();

        System.out.println("修复测试 - 管理员提交响应: " + adminResult.getResponse().getStatus()
                + " - " + adminResult.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("修复模板内容保存问题 - 简化版")
    public void testFixContentSaveSimplified() throws Exception {
        // 获取测试用户ID
        SysUser testUser = sysUserRepository.findByLoginName("testuser");
        if (testUser == null) {
            throw new RuntimeException("测试用户 'testuser' 不存在");
        }

        String userId = testUser.getId();
        System.out.println("简化测试 - 用户ID: " + userId);

        // 创建一个新的测试模板ID
        String testTemplateId = "simple-test-id";

        // 删除可能存在的模板
        try {
            if (userTemplateRepository.existsById(testTemplateId)) {
                userTemplateRepository.deleteById(testTemplateId);
            }
        } catch (Exception e) {
            System.out.println("删除旧模板时出错: " + e.getMessage());
        }

        // 创建新模板
        try {
            UserTemplate userTemplate = new UserTemplate();
            userTemplate.setId(testTemplateId);
            userTemplate.setUserId(userId);
            userTemplate.setTemplateId("template-id");
            userTemplate.setStatus(4); // 填写中
            userTemplateRepository.save(userTemplate);
            System.out.println("简化测试 - 创建模板成功: " + testTemplateId);
        } catch (Exception e) {
            System.out.println("创建模板时出错: " + e.getMessage());
            throw e;
        }

        // 获取Token用户名，确认与测试用户名匹配
        String tokenSubject = extractSubjectFromToken(userToken);
        System.out.println("简化测试 - Token的用户名: " + tokenSubject);
        System.out.println("简化测试 - 测试用户的登录名: " + testUser.getLoginName());

        // 尝试最简单的直接测试
        try {
            String jsonContent = "{\"simple\":\"test\"}";

            MvcResult result = mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                    .header("Authorization", "Bearer " + userToken)
                    .param("id", testTemplateId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonContent))
                    .andDo(print())
                    .andReturn();

            System.out.println("简化测试 - 保存内容响应码: " + result.getResponse().getStatus());
            System.out.println("简化测试 - 保存内容响应: " + result.getResponse().getContentAsString());
        } catch (Exception e) {
            System.out.println("保存内容测试异常: " + e.getMessage());
        }

        // 验证数据库中是否保存了内容
        try {
            UserTemplate savedTemplate = userTemplateRepository.findById(testTemplateId).orElse(null);
            if (savedTemplate != null) {
                System.out.println("简化测试 - 数据库中模板内容: " + savedTemplate.getContent());
            } else {
                System.out.println("简化测试 - 未找到保存的模板");
            }
        } catch (Exception e) {
            System.out.println("查询保存模板时出错: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("使用工具方法修复保存内容问题")
    public void testSaveContentWithUtil() throws Exception {
        // 获取测试用户ID
        SysUser testUser = sysUserRepository.findByLoginName("testuser");
        if (testUser == null) {
            throw new RuntimeException("测试用户 'testuser' 不存在");
        }

        String userId = testUser.getId();
        System.out.println("工具测试 - 用户ID: " + userId);

        // 创建一个新的测试模板ID
        String testTemplateId = "util-test-id";

        // 删除可能存在的模板
        try {
            if (userTemplateRepository.existsById(testTemplateId)) {
                userTemplateRepository.deleteById(testTemplateId);
            }
        } catch (Exception e) {
            System.out.println("删除旧模板时出错: " + e.getMessage());
        }

        // 创建新模板
        try {
            UserTemplate userTemplate = new UserTemplate();
            userTemplate.setId(testTemplateId);
            userTemplate.setUserId(userId);
            userTemplate.setTemplateId("template-id");
            userTemplate.setStatus(4); // 填写中
            userTemplateRepository.save(userTemplate);
            System.out.println("工具测试 - 创建模板成功: " + testTemplateId);
        } catch (Exception e) {
            System.out.println("创建模板时出错: " + e.getMessage());
            throw e;
        }

        // 使用工具方法尝试保存内容
        MvcResult result = TestUtil.saveTemplateContent(mockMvc, userToken, testTemplateId, "工具方法测试内容");
        System.out.println("工具测试 - 响应状态: " + result.getResponse().getStatus());
        System.out.println("工具测试 - 响应内容: " + result.getResponse().getContentAsString());

        // 验证数据库中是否保存了内容
        try {
            UserTemplate savedTemplate = userTemplateRepository.findById(testTemplateId).orElse(null);
            if (savedTemplate != null) {
                System.out.println("工具测试 - 数据库中模板内容: " + savedTemplate.getContent());
            } else {
                System.out.println("工具测试 - 未找到保存的模板");
            }
        } catch (Exception e) {
            System.out.println("查询保存模板时出错: " + e.getMessage());
        }
    }
}