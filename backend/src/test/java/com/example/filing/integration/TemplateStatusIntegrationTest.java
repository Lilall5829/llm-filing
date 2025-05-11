package com.example.filing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.repository.UserTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TemplateStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserTemplateRepository userTemplateRepository;

    @Autowired
    private TemplateRegistryRepository templateRegistryRepository;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    private SysUser normalUser;
    private SysUser adminUser;
    private TemplateRegistry testTemplate;
    private UserTemplate userTemplate;

    @BeforeEach
    public void setup() {
        // 确保数据库表存在
        ensureTablesExist();

        // 清空测试数据（只有在表确实存在时）
        try {
            userTemplateRepository.deleteAll();
            templateRegistryRepository.deleteAll();

            // 查找并删除指定登录名的用户
            List<SysUser> usersToDelete = userRepository.findAll().stream()
                    .filter(user -> "testuser".equals(user.getLoginName())
                            || "adminuser".equals(user.getLoginName()))
                    .toList();

            if (!usersToDelete.isEmpty()) {
                userRepository.deleteAll(usersToDelete);
            }
        } catch (Exception e) {
            // 忽略表不存在的异常，因为我们已经创建了表
        }

        // 创建测试普通用户
        normalUser = new SysUser();
        normalUser.setLoginName("testuser");
        normalUser.setUserName("Test User");
        normalUser.setPassword("password");
        normalUser.setRole(2); // 普通用户
        normalUser.setStatus(1); // 激活状态
        // 设置审计字段
        LocalDateTime now = LocalDateTime.now();
        normalUser.setCreateTime(now);
        normalUser.setUpdateTime(now);
        normalUser = userRepository.save(normalUser);

        // 创建测试管理员用户
        adminUser = new SysUser();
        adminUser.setLoginName("adminuser");
        adminUser.setUserName("Admin User");
        adminUser.setPassword("password");
        adminUser.setRole(1); // 管理员
        adminUser.setStatus(1); // 激活状态
        // 设置审计字段
        adminUser.setCreateTime(now);
        adminUser.setUpdateTime(now);
        adminUser = userRepository.save(adminUser);

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
        userTemplate = new UserTemplate();
        userTemplate.setUserId(normalUser.getId());
        userTemplate.setTemplateId(testTemplate.getId());
        userTemplate.setStatus(UserTemplateStatus.FILLING); // 填写中状态，便于测试
        userTemplate.setContent("{\"formData\":{\"name\":\"张三\"}}");
        // 设置审计字段
        userTemplate.setCreateTime(now);
        userTemplate.setUpdateTime(now);
        userTemplate = userTemplateRepository.save(userTemplate);
    }

    /**
     * 确保测试所需的表都已创建
     */
    private void ensureTablesExist() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 检查表是否存在，不存在则创建
        try {
            // 检查sys_user表
            jdbcTemplate.execute("SELECT 1 FROM sys_user WHERE 1=0");
        } catch (DataAccessException e) {
            // 表不存在，创建表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "login_name VARCHAR(50) NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "user_name VARCHAR(50) NOT NULL," +
                    "role INT NOT NULL," +
                    "status INT DEFAULT 1," +
                    "create_time TIMESTAMP," + // 允许为空
                    "update_time TIMESTAMP," + // 允许为空
                    "last_login_time TIMESTAMP" +
                    ")");
        }

        try {
            // 检查template_registry表
            jdbcTemplate.execute("SELECT 1 FROM template_registry WHERE 1=0");
        } catch (DataAccessException e) {
            // 表不存在，创建表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS template_registry (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "template_code VARCHAR(50)," +
                    "template_name VARCHAR(100) NOT NULL," +
                    "template_description TEXT," +
                    "template_type VARCHAR(50)," +
                    "template_content LONGTEXT," +
                    "file_path VARCHAR(255)," +
                    "create_by VARCHAR(36)," +
                    "update_by VARCHAR(36)," +
                    "create_time TIMESTAMP," + // 允许为空
                    "update_time TIMESTAMP," + // 允许为空
                    "deleted INT DEFAULT 0" +
                    ")");
        }

        try {
            // 检查user_template表
            jdbcTemplate.execute("SELECT 1 FROM user_template WHERE 1=0");
        } catch (DataAccessException e) {
            // 表不存在，创建表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_template (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "user_id VARCHAR(36) NOT NULL," +
                    "template_id VARCHAR(36) NOT NULL," +
                    "content LONGTEXT," +
                    "status INT DEFAULT 0," +
                    "remarks TEXT," +
                    "create_time TIMESTAMP," + // 允许为空
                    "update_time TIMESTAMP" + // 允许为空
                    ")");
        }
    }

    @Test
    public void testUpdateStatus_AsUser_FromFillingToUnderReview() throws Exception {
        // 用户将状态从填写中(4)变更为审核中(5) - 应该成功
        String remarks = "提交审核";

        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .with(csrf())
                .with(user(normalUser.getId()).roles("USER"))
                .param("id", userTemplate.getId())
                .param("status", String.valueOf(UserTemplateStatus.UNDER_REVIEW))
                .contentType(MediaType.APPLICATION_JSON)
                .content(remarks))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("状态更新成功"));

        // 验证数据库中状态已更新
        Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
        assertTrue(updatedTemplate.isPresent());
        assertEquals(UserTemplateStatus.UNDER_REVIEW, updatedTemplate.get().getStatus());
        assertEquals(remarks, updatedTemplate.get().getRemarks());
    }

    @Test
    public void testUpdateStatus_AsUser_Unauthorized() throws Exception {
        // 用户尝试将状态从填写中(4)直接变更为审核通过(6) - 应该失败
        String remarks = "尝试审核通过";

        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .with(csrf())
                .with(user(normalUser.getId()).roles("USER"))
                .param("id", userTemplate.getId())
                .param("status", String.valueOf(UserTemplateStatus.REVIEW_APPROVED))
                .contentType(MediaType.APPLICATION_JSON)
                .content(remarks))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("当前状态不允许变更为目标状态"));

        // 验证数据库中状态未变更
        Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
        assertTrue(updatedTemplate.isPresent());
        assertEquals(UserTemplateStatus.FILLING, updatedTemplate.get().getStatus());
    }

    @Test
    public void testUpdateStatus_AsUser_AnotherUserTemplate() throws Exception {
        // 创建另一个用户的模板关系
        UserTemplate anotherUserTemplate = new UserTemplate();
        anotherUserTemplate.setUserId(adminUser.getId()); // 属于管理员用户
        anotherUserTemplate.setTemplateId(testTemplate.getId());
        anotherUserTemplate.setStatus(UserTemplateStatus.FILLING);
        // 设置审计字段
        LocalDateTime now = LocalDateTime.now();
        anotherUserTemplate.setCreateTime(now);
        anotherUserTemplate.setUpdateTime(now);
        anotherUserTemplate = userTemplateRepository.save(anotherUserTemplate);

        // 普通用户尝试更新其他用户的模板状态 - 应该失败
        String remarks = "尝试更新他人模板";

        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .with(csrf())
                .with(user(normalUser.getId()).roles("USER"))
                .param("id", anotherUserTemplate.getId())
                .param("status", String.valueOf(UserTemplateStatus.UNDER_REVIEW))
                .contentType(MediaType.APPLICATION_JSON)
                .content(remarks))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("无权操作他人的模板"));

        // 验证数据库中状态未变更
        Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(anotherUserTemplate.getId());
        assertTrue(updatedTemplate.isPresent());
        assertEquals(UserTemplateStatus.FILLING, updatedTemplate.get().getStatus());
    }

    @Test
    public void testUpdateStatus_AsAdmin_AnyStatus() throws Exception {
        // 管理员将用户模板从填写中(4)直接变更为审核通过(6) - 应该成功
        String remarks = "管理员审核通过";

        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .with(csrf())
                .with(user(adminUser.getId()).roles("ADMIN"))
                .param("id", userTemplate.getId())
                .param("status", String.valueOf(UserTemplateStatus.REVIEW_APPROVED))
                .contentType(MediaType.APPLICATION_JSON)
                .content(remarks))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("状态更新成功"));

        // 验证数据库中状态已更新
        Optional<UserTemplate> updatedTemplate = userTemplateRepository.findById(userTemplate.getId());
        assertTrue(updatedTemplate.isPresent());
        assertEquals(UserTemplateStatus.REVIEW_APPROVED, updatedTemplate.get().getStatus());
        assertEquals(remarks, updatedTemplate.get().getRemarks());
    }

    @Test
    public void testUpdateStatus_TemplateNotFound() throws Exception {
        // 尝试更新不存在的模板状态
        String nonExistentId = "non-existent-id";
        String remarks = "状态更新";

        mockMvc.perform(post("/api/userTemplate/updateTemplateStatus")
                .with(csrf())
                .with(user(normalUser.getId()).roles("USER"))
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