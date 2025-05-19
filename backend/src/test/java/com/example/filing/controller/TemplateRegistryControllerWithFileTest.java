package com.example.filing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.config.TestSecurityConfig;
import com.example.filing.dto.request.TemplateRegistryRequest;
import com.example.filing.security.JwtAuthenticationFilter;
import com.example.filing.service.FileStorageService;
import com.example.filing.service.TemplateRegistryService;
import com.example.filing.service.WordDocumentService;
import com.example.filing.util.JwtTokenUtil;
import com.example.filing.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@WebMvcTest(controllers = TemplateRegistryController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@SuppressWarnings({ "unchecked", "rawtypes" })
public class TemplateRegistryControllerWithFileTest {

        @Configuration
        static class TestConfig {
                @Bean
                public TemplateRegistryController templateRegistryController(
                                TemplateRegistryService templateRegistryService,
                                FileStorageService fileStorageService,
                                WordDocumentService wordDocumentService,
                                ObjectMapper objectMapper) {

                        return new TemplateRegistryController(templateRegistryService,
                                        fileStorageService, wordDocumentService, objectMapper) {

                                @Override
                                public ResponseEntity<Result<?>> saveTemplateRegistry(
                                                @RequestParam(value = "file", required = false) MultipartFile file,
                                                @RequestParam(value = "data", required = false) String data,
                                                @RequestParam(value = "id", required = false) String id,
                                                @RequestParam(value = "templateCode", required = false) String templateCode,
                                                @RequestParam(value = "templateName", required = false) String templateName,
                                                @RequestParam(value = "templateDescription", required = false) String templateDescription,
                                                @RequestParam(value = "templateType", required = false) String templateType,
                                                @RequestParam(value = "templateContent", required = false) String templateContent,
                                                Authentication authentication) {
                                        // 处理测试中的请求数据
                                        TemplateRegistryRequest finalRequest = new TemplateRegistryRequest();

                                        // 从data参数解析JSON
                                        if (data != null && !data.trim().isEmpty()) {
                                                try {
                                                        finalRequest = objectMapper.readValue(data,
                                                                        TemplateRegistryRequest.class);
                                                } catch (Exception e) {
                                                        return ResponseEntity.badRequest()
                                                                        .contentType(MediaType.APPLICATION_JSON)
                                                                        .body(Result.failed("无效的JSON数据: "
                                                                                        + e.getMessage()));
                                                }
                                        } else {
                                                // 使用单独的参数构建请求对象
                                                if (id != null)
                                                        finalRequest.setId(id);
                                                if (templateCode != null)
                                                        finalRequest.setTemplateCode(templateCode);
                                                if (templateName != null)
                                                        finalRequest.setTemplateName(templateName);
                                                if (templateDescription != null)
                                                        finalRequest.setTemplateDescription(templateDescription);
                                                if (templateType != null)
                                                        finalRequest.setTemplateType(templateType);
                                                if (templateContent != null)
                                                        finalRequest.setTemplateContent(templateContent);
                                        }

                                        return ResponseEntity.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(templateRegistryService.saveTemplateRegistry(finalRequest,
                                                                        "test-admin-id"));
                                }

                                @Override
                                public ResponseEntity<Result<?>> uploadTemplateFile(
                                                MultipartFile file, String templateId) {
                                        try {
                                                // Process file
                                                String fileName = fileStorageService.storeFile(file, "templates");
                                                String templateContent = wordDocumentService.parseWordDocument(file);

                                                // Update template file path
                                                Result<?> pathUpdateResult = templateRegistryService
                                                                .updateTemplateFilePath(templateId,
                                                                                "templates/" + fileName);
                                                if (pathUpdateResult.getCode() != 200) {
                                                        return ResponseEntity.badRequest()
                                                                        .contentType(MediaType.APPLICATION_JSON)
                                                                        .body(pathUpdateResult);
                                                }

                                                // Update template content
                                                Result<?> contentUpdateResult = templateRegistryService
                                                                .updateTemplateContent(templateId, templateContent);
                                                if (contentUpdateResult.getCode() != 200) {
                                                        return ResponseEntity.badRequest()
                                                                        .contentType(MediaType.APPLICATION_JSON)
                                                                        .body(contentUpdateResult);
                                                }

                                                // Create response
                                                return ResponseEntity.ok()
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .body(Result.success("模板文件上传成功",
                                                                                java.util.Map.of(
                                                                                                "fileName", fileName,
                                                                                                "filePath",
                                                                                                "templates/" + fileName,
                                                                                                "success", true)));
                                        } catch (Exception e) {
                                                return ResponseEntity.badRequest()
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .body(Result.failed(e.getMessage()));
                                        }
                                }
                        };
                }
        }

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private TemplateRegistryService templateRegistryService;

        @MockBean
        private FileStorageService fileStorageService;

        @MockBean
        private WordDocumentService wordDocumentService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockBean
        private JwtTokenUtil jwtTokenUtil;

        private MockMultipartFile wordFile;
        private Result updatePathResult;
        private Result updateContentResult;

        @PostConstruct
        public void setupMockMvc() {
                this.mockMvc = MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build();
        }

        @BeforeEach
        void setup() {
                // 创建测试文件
                wordFile = new MockMultipartFile(
                                "file",
                                "test.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "Test document content".getBytes());

                // 准备更新结果
                updatePathResult = Result.success("模板文件路径更新成功");
                updateContentResult = Result.success("模板内容更新成功");

                // 配置Mock服务 - 使用doReturn/when替代when/thenReturn避免NPE
                org.mockito.Mockito.doReturn(true).when(wordDocumentService)
                                .isValidWordDocument(any(MultipartFile.class));
                org.mockito.Mockito.doReturn("uuid-test.docx").when(fileStorageService)
                                .storeFile(any(MultipartFile.class), eq("templates"));
                org.mockito.Mockito.doReturn("{\"formFields\":[{\"name\":\"field1\",\"type\":\"text\"}]}")
                                .when(wordDocumentService).parseWordDocument(any(MultipartFile.class));
                org.mockito.Mockito.doReturn(updatePathResult).when(templateRegistryService)
                                .updateTemplateFilePath(anyString(), anyString());
                org.mockito.Mockito.doReturn(updateContentResult).when(templateRegistryService)
                                .updateTemplateContent(anyString(), anyString());
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        void testUploadTemplateFile_Success() throws Exception {
                // 执行请求
                ResultActions result = mockMvc.perform(multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(wordFile)
                                .param("templateId", "template123")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print());

                // 验证结果
                result.andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.fileName").exists())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        void testUploadTemplateFile_Forbidden() throws Exception {
                // 执行请求
                mockMvc.perform(multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(wordFile)
                                .param("templateId", "template123")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        void testUploadTemplateFile_EmptyFile() throws Exception {
                // 创建空文件
                MockMultipartFile emptyFile = new MockMultipartFile(
                                "file",
                                "empty.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                new byte[0]);

                // 执行请求
                mockMvc.perform(multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(emptyFile)
                                .param("templateId", "template123")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        void testUploadTemplateFile_InvalidFileType() throws Exception {
                // 设置文件验证为失败 - 使用doReturn/when替代when/thenReturn
                org.mockito.Mockito.doReturn(false).when(wordDocumentService)
                                .isValidWordDocument(any(MultipartFile.class));

                // 执行请求
                mockMvc.perform(multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(wordFile)
                                .param("templateId", "template123")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        void testUploadTemplateFile_UpdatePathFailed() throws Exception {
                // 设置路径更新失败
                when(templateRegistryService.updateTemplateFilePath(anyString(), anyString()))
                                .thenReturn(Result.failed("模板不存在"));

                // 执行请求
                mockMvc.perform(multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(wordFile)
                                .param("templateId", "template123")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("模板不存在"));
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        void testUploadTemplateFile_UpdateContentFailed() throws Exception {
                // 设置内容更新失败
                when(templateRegistryService.updateTemplateContent(anyString(), anyString()))
                                .thenReturn(Result.failed("更新内容失败"));

                // 执行请求
                mockMvc.perform(multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(wordFile)
                                .param("templateId", "template123")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("更新内容失败"));
        }
}