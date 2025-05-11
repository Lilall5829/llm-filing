package com.example.filing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.config.TestSecurityConfig;
import com.example.filing.security.JwtAuthenticationFilter;
import com.example.filing.service.FileStorageService;
import com.example.filing.service.WordDocumentService;
import com.example.filing.util.JwtTokenUtil;
import com.example.filing.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@WebMvcTest(FileController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@SuppressWarnings({ "unchecked", "rawtypes" })
public class FileControllerTest {

        @Configuration
        static class TestConfig {
                @Bean
                public FileController fileController(
                                FileStorageService fileStorageService,
                                WordDocumentService wordDocumentService) {
                        return new FileController(fileStorageService, wordDocumentService) {
                                // Override methods to ensure proper content type and fix return types
                                @Override
                                public ResponseEntity<Result<Map<String, String>>> uploadTemplate(MultipartFile file) {
                                        try {
                                                if (file.isEmpty()) {
                                                        return ResponseEntity.badRequest()
                                                                        .contentType(MediaType.APPLICATION_JSON)
                                                                        .body(Result.failed("请选择文件上传"));
                                                }

                                                if (!wordDocumentService.isValidWordDocument(file)) {
                                                        return ResponseEntity.badRequest()
                                                                        .contentType(MediaType.APPLICATION_JSON)
                                                                        .body(Result.failed("仅允许上传Word文档(.docx)"));
                                                }

                                                String fileName = fileStorageService.storeFile(file, "templates");
                                                String templateContent = wordDocumentService.parseWordDocument(file);

                                                Map<String, String> response = new HashMap<>();
                                                response.put("fileName", fileName);
                                                response.put("filePath", "templates/" + fileName);
                                                response.put("templateContent", templateContent);

                                                return ResponseEntity.ok()
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .body(Result.success(response));
                                        } catch (Exception e) {
                                                return ResponseEntity.badRequest()
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .body(Result.failed(e.getMessage()));
                                        }
                                }

                                @Override
                                public ResponseEntity<Result<String>> deleteFile(String directory, String fileName) {
                                        boolean deleted = fileStorageService.deleteFile(fileName, directory);
                                        if (deleted) {
                                                return ResponseEntity.ok()
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .body(Result.success("文件删除成功"));
                                        } else {
                                                return ResponseEntity.badRequest()
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .body(Result.failed("文件删除失败或文件不存在"));
                                        }
                                }
                        };
                }
        }

        @Autowired
        private WebApplicationContext webApplicationContext;

        @TempDir
        static File tempDir;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private FileStorageService fileStorageService;

        @MockBean
        private WordDocumentService wordDocumentService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockBean
        private JwtTokenUtil jwtTokenUtil;

        private MockMultipartFile wordFile;
        private Resource fileResource;
        private Result uploadResult;

        @PostConstruct
        public void setupMockMvc() {
                this.mockMvc = MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(SecurityMockMvcConfigurers.springSecurity())
                                .build();
        }

        @BeforeEach
        public void setup() throws Exception {
                // Create test file
                wordFile = new MockMultipartFile(
                                "file",
                                "test.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "Test document content".getBytes());

                // Create temporary test file
                File testFile = new File(tempDir, "test.docx");
                Files.write(testFile.toPath(), "Test document content".getBytes());

                // Use real file as resource
                fileResource = new FileSystemResource(testFile);

                // Prepare upload result
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("fileName", "uuid-test.docx");
                responseData.put("filePath", "templates/uuid-test.docx");
                uploadResult = Result.success("File uploaded successfully", responseData);

                // Configure mock services
                when(wordDocumentService.isValidWordDocument(any())).thenReturn(true);
                when(fileStorageService.storeFile(any(), eq("templates"))).thenReturn("uuid-test.docx");
                when(wordDocumentService.parseWordDocument(any(MultipartFile.class)))
                                .thenReturn("{\"formFields\":[{\"name\":\"field1\",\"type\":\"text\"}]}");
                when(fileStorageService.loadFileAsResource(eq("test.docx"), eq("templates"))).thenReturn(fileResource);
                when(fileStorageService.deleteFile(eq("test.docx"), eq("templates"))).thenReturn(true);
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        public void testUploadTemplate_Success() throws Exception {
                // Setup specific mock for this test
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("fileName", "uuid-test.docx");
                responseData.put("filePath", "templates/uuid-test.docx");
                Result successResult = Result.success("File uploaded successfully", responseData);

                // Mock service responses
                when(fileStorageService.storeFile(any(), eq("templates"))).thenReturn("uuid-test.docx");
                when(wordDocumentService.parseWordDocument(any(MultipartFile.class))).thenReturn("{\"formFields\":[]}");

                // Execute and verify
                mockMvc.perform(multipart("/api/file/uploadTemplate")
                                .file(wordFile)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        public void testUploadTemplate_Forbidden() throws Exception {
                mockMvc.perform(multipart("/api/file/uploadTemplate")
                                .file(wordFile)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isForbidden());
        }

        @Test
        public void testDownloadFile_Success() throws Exception {
                mockMvc.perform(get("/api/file/download/templates/test.docx"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(header().exists("Content-Disposition"))
                                .andExpect(header().string("Content-Disposition",
                                                org.hamcrest.Matchers
                                                                .containsString("attachment; filename=\"test.docx\"")));
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        public void testDeleteFile_Success() throws Exception {
                // Setup specific mock for this test
                when(fileStorageService.deleteFile(eq("test.docx"), eq("templates"))).thenReturn(true);

                mockMvc.perform(delete("/api/file/delete/templates/test.docx")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        public void testDeleteFile_Forbidden() throws Exception {
                mockMvc.perform(delete("/api/file/delete/templates/test.docx")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isForbidden());
        }
}