package com.example.filing.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.filing.constants.UserTemplateStatus;
import com.example.filing.entity.SysUser;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.entity.UserTemplate;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.repository.UserTemplateRepository;
import com.example.filing.util.Result;

class UserTemplateServiceImplTest {

    @Mock
    private UserTemplateRepository userTemplateRepository;

    @Mock
    private TemplateRegistryRepository templateRegistryRepository;

    @Mock
    private SysUserRepository sysUserRepository;

    @InjectMocks
    private UserTemplateServiceImpl userTemplateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testApplyTemplate_AsUser_Success() {
        // 准备测试数据
        String templateId = "template-123";
        List<String> userIds = Arrays.asList("user-123");
        String operatorId = "user-123";
        boolean isAdmin = false;

        // 模拟模板存在
        TemplateRegistry template = new TemplateRegistry();
        template.setId(templateId);
        template.setTemplateName("测试模板");
        when(templateRegistryRepository.findById(templateId)).thenReturn(Optional.of(template));

        // 模拟用户存在
        SysUser user = new SysUser();
        user.setId("user-123");
        user.setUserName("测试用户");
        when(sysUserRepository.findAllById(userIds)).thenReturn(Arrays.asList(user));

        // 模拟保存关系成功
        UserTemplate savedRelation = new UserTemplate();
        savedRelation.setId("relation-123");
        savedRelation.setUserId("user-123");
        savedRelation.setTemplateId(templateId);
        savedRelation.setStatus(UserTemplateStatus.PENDING_APPROVAL); // 用户申请初始状态为待审核(0)
        when(userTemplateRepository.saveAll(anyList())).thenReturn(Arrays.asList(savedRelation));

        // 执行测试
        Result<List<String>> result = userTemplateService.applyTemplate(templateId, userIds, operatorId, isAdmin);

        // 验证结果
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
        assertEquals("relation-123", result.getData().get(0));
        assertTrue(result.getMessage().contains("申请成功"));
    }

    @Test
    void testApplyTemplate_AsAdmin_Success() {
        // 准备测试数据
        String templateId = "template-123";
        List<String> userIds = Arrays.asList("user-123", "user-456");
        String operatorId = "admin-123";
        boolean isAdmin = true;

        // 模拟模板存在
        TemplateRegistry template = new TemplateRegistry();
        template.setId(templateId);
        template.setTemplateName("测试模板");
        when(templateRegistryRepository.findById(templateId)).thenReturn(Optional.of(template));

        // 模拟用户存在
        SysUser user1 = new SysUser();
        user1.setId("user-123");
        user1.setUserName("测试用户1");

        SysUser user2 = new SysUser();
        user2.setId("user-456");
        user2.setUserName("测试用户2");

        when(sysUserRepository.findAllById(userIds)).thenReturn(Arrays.asList(user1, user2));

        // 模拟保存关系成功
        UserTemplate savedRelation1 = new UserTemplate();
        savedRelation1.setId("relation-123");
        savedRelation1.setUserId("user-123");
        savedRelation1.setTemplateId(templateId);
        savedRelation1.setStatus(UserTemplateStatus.PENDING_FILL); // 管理员发送初始状态为待填写(3)

        UserTemplate savedRelation2 = new UserTemplate();
        savedRelation2.setId("relation-456");
        savedRelation2.setUserId("user-456");
        savedRelation2.setTemplateId(templateId);
        savedRelation2.setStatus(UserTemplateStatus.PENDING_FILL); // 管理员发送初始状态为待填写(3)

        when(userTemplateRepository.saveAll(anyList())).thenReturn(Arrays.asList(savedRelation1, savedRelation2));

        // 执行测试
        Result<List<String>> result = userTemplateService.applyTemplate(templateId, userIds, operatorId, isAdmin);

        // 验证结果
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertTrue(result.getMessage().contains("发送成功"));
    }

    @Test
    void testApplyTemplate_TemplateNotFound() {
        // 准备测试数据
        String templateId = "non-existent-template";
        List<String> userIds = Arrays.asList("user-123");
        String operatorId = "user-123";
        boolean isAdmin = false;

        // 模拟模板不存在
        when(templateRegistryRepository.findById(templateId)).thenReturn(Optional.empty());

        // 执行测试
        Result<List<String>> result = userTemplateService.applyTemplate(templateId, userIds, operatorId, isAdmin);

        // 验证结果
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("模板不存在"));
    }

    @Test
    void testApplyTemplate_UserNotFound() {
        // 准备测试数据
        String templateId = "template-123";
        List<String> userIds = Arrays.asList("non-existent-user");
        String operatorId = "user-123";
        boolean isAdmin = true;

        // 模拟模板存在
        TemplateRegistry template = new TemplateRegistry();
        template.setId(templateId);
        template.setTemplateName("测试模板");
        when(templateRegistryRepository.findById(templateId)).thenReturn(Optional.of(template));

        // 模拟用户不存在
        when(sysUserRepository.findAllById(userIds)).thenReturn(Arrays.asList());

        // 执行测试
        Result<List<String>> result = userTemplateService.applyTemplate(templateId, userIds, operatorId, isAdmin);

        // 验证结果
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("部分用户不存在"));
    }

    @Test
    void testGetTemplateContent_Success() {
        // 准备测试数据
        String userTemplateId = "template-relation-123";
        String savedContent = "{\"formData\":{\"field1\":\"value1\"}}";

        // 创建一个UserTemplate，并设置内容
        UserTemplate userTemplate = new UserTemplate();
        userTemplate.setId(userTemplateId);
        userTemplate.setUserId("user-123");
        userTemplate.setTemplateId("template-123");
        userTemplate.setContent(savedContent);
        userTemplate.setStatus(UserTemplateStatus.FILLING);

        // 模拟数据库查询
        when(userTemplateRepository.findById(userTemplateId)).thenReturn(Optional.of(userTemplate));

        // 执行测试
        Result<String> result = userTemplateService.getTemplateContent(userTemplateId);

        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(savedContent, result.getData());
    }

    @Test
    void testGetTemplateContent_NotFound() {
        // 准备测试数据
        String userTemplateId = "non-existent-relation";

        // 模拟数据库查询 - 找不到记录
        when(userTemplateRepository.findById(userTemplateId)).thenReturn(Optional.empty());

        // 执行测试
        Result<String> result = userTemplateService.getTemplateContent(userTemplateId);

        // 验证结果
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("不存在"));
    }

    @Test
    void testSaveTemplateContent_Success() {
        // 准备测试数据
        String userTemplateId = "template-relation-123";
        String userId = "user-123";
        String newContent = "{\"formData\":{\"field1\":\"updated-value\"}}";

        // 创建一个UserTemplate
        UserTemplate userTemplate = new UserTemplate();
        userTemplate.setId(userTemplateId);
        userTemplate.setUserId(userId);
        userTemplate.setTemplateId("template-123");
        userTemplate.setContent("{\"formData\":{\"field1\":\"old-value\"}}");
        userTemplate.setStatus(UserTemplateStatus.FILLING);

        // 模拟数据库查询
        when(userTemplateRepository.findById(userTemplateId)).thenReturn(Optional.of(userTemplate));
        when(userTemplateRepository.save(any(UserTemplate.class))).thenAnswer(i -> i.getArgument(0));

        // 执行测试
        Result<String> result = userTemplateService.saveTemplateContent(userTemplateId, newContent, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("内容保存成功", result.getMessage());
        assertEquals(newContent, userTemplate.getContent());
    }

    @Test
    void testSaveTemplateContent_StatusTransition() {
        // 准备测试数据
        String userTemplateId = "template-relation-123";
        String userId = "user-123";
        String newContent = "{\"formData\":{\"field1\":\"new-value\"}}";

        // 创建一个UserTemplate，状态为待填写(3)
        UserTemplate userTemplate = new UserTemplate();
        userTemplate.setId(userTemplateId);
        userTemplate.setUserId(userId);
        userTemplate.setTemplateId("template-123");
        userTemplate.setContent(null); // 初始没有内容
        userTemplate.setStatus(UserTemplateStatus.PENDING_FILL); // 状态为待填写

        // 模拟数据库查询
        when(userTemplateRepository.findById(userTemplateId)).thenReturn(Optional.of(userTemplate));
        when(userTemplateRepository.save(any(UserTemplate.class))).thenAnswer(i -> i.getArgument(0));

        // 执行测试
        Result<String> result = userTemplateService.saveTemplateContent(userTemplateId, newContent, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(newContent, userTemplate.getContent());
        // 验证状态已从待填写(3)变更为填写中(4)
        assertEquals(UserTemplateStatus.FILLING, userTemplate.getStatus());
    }

    @Test
    void testSaveTemplateContent_Unauthorized() {
        // 准备测试数据
        String userTemplateId = "template-relation-123";
        String templateUserId = "user-123";
        String otherUserId = "user-456"; // 另一个用户ID
        String newContent = "{\"formData\":{\"field1\":\"updated-value\"}}";

        // 创建一个UserTemplate
        UserTemplate userTemplate = new UserTemplate();
        userTemplate.setId(userTemplateId);
        userTemplate.setUserId(templateUserId); // 模板归属于user-123
        userTemplate.setTemplateId("template-123");
        userTemplate.setContent("{\"formData\":{\"field1\":\"old-value\"}}");
        userTemplate.setStatus(UserTemplateStatus.FILLING);

        // 模拟数据库查询
        when(userTemplateRepository.findById(userTemplateId)).thenReturn(Optional.of(userTemplate));

        // 执行测试 - 使用其他用户ID尝试保存内容
        Result<String> result = userTemplateService.saveTemplateContent(userTemplateId, newContent, otherUserId);

        // 验证结果
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("无权操作他人的模板"));
    }
}