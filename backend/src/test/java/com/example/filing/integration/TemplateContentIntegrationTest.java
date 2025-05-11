package com.example.filing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.service.UserTemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
// Remove the SQL execution from class level, we'll use hibernate auto-creation
// instead
public class TemplateContentIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserTemplateRepository userTemplateRepository;

        @Autowired
        private TemplateRegistryRepository templateRegistryRepository;

        @Autowired
        private SysUserRepository userRepository;

        @Autowired
        private UserTemplateService userTemplateService;

        @Autowired
        private ObjectMapper objectMapper;

        private SysUser testUser;
        private TemplateRegistry testTemplate;
        private UserTemplate testUserTemplate;

        @BeforeEach
        public void setup() {
                // 清空测试数据
                userTemplateRepository.deleteAll();
                templateRegistryRepository.deleteAll();
                // 删除特定登录名的用户
                userRepository.deleteAll(userRepository.findAll().stream()
                                .filter(user -> "testuser".equals(user.getLoginName())
                                                || "anotheruser".equals(user.getLoginName()))
                                .collect(java.util.stream.Collectors.toList()));

                // 创建测试用户
                testUser = new SysUser();
                testUser.setLoginName("testuser");
                testUser.setUserName("Test User");
                testUser.setPassword("password"); // 实际项目中应该加密
                testUser.setRole(2); // 普通用户
                testUser.setStatus(1); // 激活状态

                // 设置审计字段
                LocalDateTime now = LocalDateTime.now();
                testUser.setCreateTime(now);
                testUser.setUpdateTime(now);

                testUser = userRepository.save(testUser);

                // 创建测试模板
                testTemplate = new TemplateRegistry();
                testTemplate.setTemplateName("Test Template");
                testTemplate.setTemplateType("TEST");
                testTemplate.setTemplateContent(
                                "{\"sections\":[{\"name\":\"基本信息\",\"fields\":[{\"label\":\"姓名\",\"type\":\"text\",\"required\":true}]}]}");

                // 设置审计字段
                testTemplate.setCreateTime(now);
                testTemplate.setUpdateTime(now);
                testTemplate.setDeleted(0);

                testTemplate = templateRegistryRepository.save(testTemplate);

                // 创建用户模板关系
                testUserTemplate = new UserTemplate();
                testUserTemplate.setUserId(testUser.getId());
                testUserTemplate.setTemplateId(testTemplate.getId());
                testUserTemplate.setStatus(UserTemplateStatus.PENDING_FILL); // 待填写状态

                // 设置审计字段
                testUserTemplate.setCreateTime(now);
                testUserTemplate.setUpdateTime(now);

                testUserTemplate = userTemplateRepository.save(testUserTemplate);
        }

        @Test
        public void testGetTemplateContent_EmptyContent() throws Exception {
                // 获取模板内容 - 初始应该为空
                MvcResult result = mockMvc.perform(get("/api/userTemplate/getTemplateContent")
                                .param("id", testUserTemplate.getId())
                                .with(user(testUser.getId()).roles("USER"))
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andReturn();

                String responseContent = result.getResponse().getContentAsString();
                JsonNode responseJson = objectMapper.readTree(responseContent);

                // 初始内容应为null
                assertTrue(responseJson.has("data"));
                assertTrue(responseJson.get("data").isNull() || responseJson.get("data").asText().isEmpty());
        }

        @Test
        public void testSaveAndGetTemplateContent() throws Exception {
                // 准备要保存的内容
                String contentToSave = "{\"formData\":{\"name\":\"张三\",\"age\":30}}";

                // 保存内容
                mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                                .with(csrf())
                                .with(user(testUser.getId()).roles("USER"))
                                .param("id", testUserTemplate.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contentToSave))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("内容保存成功"));

                // 验证数据库中的状态已更新为"填写中"
                Optional<UserTemplate> updatedTemplateOpt = userTemplateRepository.findById(testUserTemplate.getId());
                assertTrue(updatedTemplateOpt.isPresent());
                UserTemplate updatedTemplate = updatedTemplateOpt.get();
                assertEquals(UserTemplateStatus.FILLING, updatedTemplate.getStatus());
                assertEquals(contentToSave, updatedTemplate.getContent());

                // 获取保存的内容
                MvcResult getResult = mockMvc.perform(get("/api/userTemplate/getTemplateContent")
                                .param("id", testUserTemplate.getId())
                                .with(user(testUser.getId()).roles("USER"))
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").value(contentToSave))
                                .andReturn();
        }

        @Test
        public void testSaveTemplateContent_Unauthorized() throws Exception {
                // 创建另一个用户
                SysUser anotherUser = new SysUser();
                anotherUser.setLoginName("anotheruser");
                anotherUser.setUserName("Another User");
                anotherUser.setPassword("password");
                anotherUser.setRole(2);
                anotherUser.setStatus(1);

                // 设置审计字段
                LocalDateTime now = LocalDateTime.now();
                anotherUser.setCreateTime(now);
                anotherUser.setUpdateTime(now);

                anotherUser = userRepository.save(anotherUser);

                // 准备要保存的内容
                String contentToSave = "{\"formData\":{\"name\":\"李四\",\"age\":25}}";

                // 使用另一个用户尝试保存内容
                mockMvc.perform(post("/api/userTemplate/saveTemplateContent")
                                .with(csrf())
                                .with(user(anotherUser.getId()).roles("USER"))
                                .param("id", testUserTemplate.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contentToSave))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("无权操作他人的模板"));

                // 验证数据库中的内容和状态未变
                Optional<UserTemplate> updatedTemplateOpt = userTemplateRepository.findById(testUserTemplate.getId());
                assertTrue(updatedTemplateOpt.isPresent());
                UserTemplate updatedTemplate = updatedTemplateOpt.get();
                assertEquals(UserTemplateStatus.PENDING_FILL, updatedTemplate.getStatus());
                assertEquals(null, updatedTemplate.getContent());
        }
}