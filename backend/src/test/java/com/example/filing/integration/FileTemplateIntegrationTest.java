package com.example.filing.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.filing.config.TestConfig;
import com.example.filing.config.TestSecurityConfig;
import com.example.filing.dto.request.TemplateRegistryRequest;
import com.example.filing.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestConfig.class, TestSecurityConfig.class })
public class FileTemplateIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @TempDir
        Path tempDir;

        private MockMultipartFile wordFile;
        private TemplateRegistryRequest templateRequest;
        private String templateId;

        @BeforeEach
        public void setup() throws Exception {
                // 设置安全上下文 - 直接使用UserDetailsImpl而不是依赖@WithMockUser
                setupSecurityContext();

                // 创建Word文档
                Path docxPath = createTestWordDocument();
                byte[] docxContent = Files.readAllBytes(docxPath);

                // 创建MultipartFile
                wordFile = new MockMultipartFile(
                                "file",
                                "test-template.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                docxContent);

                // 创建模板请求
                templateRequest = new TemplateRegistryRequest();
                templateRequest.setTemplateCode("TEST" + System.currentTimeMillis());
                templateRequest.setTemplateName("集成测试模板");
                templateRequest.setTemplateDescription("这是一个用于集成测试的模板");
                templateRequest.setTemplateType("测试类型");

                // 生成唯一ID
                templateId = UUID.randomUUID().toString();
        }

        /**
         * 设置安全上下文，直接使用UserDetailsImpl
         */
        private void setupSecurityContext() {
                // 创建管理员权限
                Collection<GrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_ADMIN"));

                // 创建UserDetailsImpl实例
                UserDetailsImpl userDetails = new UserDetailsImpl(
                                "admin-test-id",
                                "Admin User",
                                "admin",
                                "password", // 密码在这里不重要，因为我们不会实际验证它
                                1,
                                authorities);

                // 创建认证对象
                Authentication auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, authorities);

                // 设置到SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(auth);

                System.out.println("已设置测试安全上下文：" + userDetails.getUsername() + ", 角色: " + userDetails.getAuthorities());
        }

        @Test
        public void testCompleteTemplateFileWorkflow() throws Exception {
                // 步骤1: 创建新模板
                MockHttpServletRequestBuilder createRequest = post("/api/templateRegistry/saveTemplateRegistry")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(templateRequest))
                                .accept(MediaType.APPLICATION_JSON);

                MvcResult createResult = mockMvc.perform(createRequest)
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andReturn();

                // 从创建结果中提取模板ID
                JsonNode responseJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
                String newTemplateId = responseJson.path("data").path("id").asText();
                assertFalse(newTemplateId.isEmpty(), "模板ID不应为空");

                // 步骤2: 上传模板文件
                MockHttpServletRequestBuilder uploadRequest = multipart("/api/templateRegistry/uploadTemplateFile")
                                .file(wordFile)
                                .param("templateId", newTemplateId)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON);

                mockMvc.perform(uploadRequest)
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.fileName").exists())
                                .andExpect(jsonPath("$.data.templateContent").exists());

                // 步骤3: 获取更新后的模板详情
                mockMvc.perform(get("/api/templateRegistry/getTemplateRegistryById")
                                .param("id", newTemplateId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.templateContent").exists())
                                .andExpect(jsonPath("$.data.filePath").exists());

                // 步骤4: 删除模板
                mockMvc.perform(delete("/api/templateRegistry/deleteTemplate")
                                .param("id", newTemplateId)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        /**
         * 创建测试用Word文档
         */
        private Path createTestWordDocument() throws Exception {
                Path docxPath = tempDir.resolve("test-template.docx");

                try (XWPFDocument document = new XWPFDocument()) {
                        // 添加标题
                        XWPFParagraph title = document.createParagraph();
                        title.setStyle("Heading1");
                        title.createRun().setText("测试模板标题");

                        // 添加内容段落
                        XWPFParagraph paragraph1 = document.createParagraph();
                        paragraph1.createRun().setText("这是一个测试模板的内容段落，用于测试文档解析功能。");

                        XWPFParagraph paragraph2 = document.createParagraph();
                        paragraph2.createRun().setText("这是第二个段落，包含一些格式化内容。");

                        // 添加表格
                        XWPFParagraph tableTitleParagraph = document.createParagraph();
                        tableTitleParagraph.setStyle("Heading2");
                        tableTitleParagraph.createRun().setText("表格示例");

                        // 保存文档
                        try (java.io.FileOutputStream out = new java.io.FileOutputStream(docxPath.toFile())) {
                                document.write(out);
                        }
                }

                return docxPath;
        }
}