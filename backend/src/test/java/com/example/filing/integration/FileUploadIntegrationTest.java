package com.example.filing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.filing.service.FileStorageService;
import com.example.filing.service.WordDocumentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 文件上传相关功能的集成测试
 * 注意：这个测试会创建实际的文件，并依赖于配置的文件上传目录
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "file.upload-dir=./test-uploads", // 测试专用上传目录
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false" // 禁用Flyway以避免数据库迁移问题
})
public class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WordDocumentService wordDocumentService;

    @TempDir
    static Path tempDir;

    private static MockMultipartFile wordFile;
    private static final String TEST_UPLOAD_DIR = "./test-uploads";

    @BeforeAll
    static void setupAll() throws Exception {
        // 创建测试上传目录
        Files.createDirectories(Paths.get(TEST_UPLOAD_DIR));
        Files.createDirectories(Paths.get(TEST_UPLOAD_DIR, "templates"));

        // 创建一个简单的Word文档用于测试
        Path docxPath = createSimpleWordDoc();
        byte[] docxContent = Files.readAllBytes(docxPath);

        // 创建测试文件
        wordFile = new MockMultipartFile(
                "file",
                "test-integration.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent);
    }

    @BeforeEach
    void setUp() throws Exception {
        // 确保测试目录存在
        Files.createDirectories(Paths.get(TEST_UPLOAD_DIR));
        Files.createDirectories(Paths.get(TEST_UPLOAD_DIR, "templates"));
    }

    /**
     * 创建一个简单的Word文档
     */
    private static Path createSimpleWordDoc() throws Exception {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().setText("This is a test document for integration testing.");

        Path docxPath = tempDir.resolve("test-doc.docx");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(docxPath.toFile())) {
            document.write(out);
        }

        return docxPath;
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void testFileUpload() throws Exception {
        // 执行请求
        MvcResult result = mockMvc.perform(multipart("/api/file/uploadTemplate")
                .file(wordFile)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // 解析响应
        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonResponse = objectMapper.readTree(responseContent);

        // 验证响应码
        assertEquals(200, jsonResponse.get("code").asInt());

        // 获取文件名
        String fileName = jsonResponse.get("data").get("fileName").asText();

        // 验证文件是否真的保存到了指定位置
        Path savedFilePath = Paths.get(TEST_UPLOAD_DIR, "templates", fileName);
        assertTrue(Files.exists(savedFilePath), "上传的文件应该存在");

        // 验证模板内容
        String templateContent = jsonResponse.get("data").get("templateContent").asText();
        assertNotNull(templateContent);
        assertTrue(templateContent.contains("sections"));
    }

    @Test
    @WithMockUser(roles = { "USER" })
    void testFileUpload_Forbidden() throws Exception {
        // 执行请求，验证普通用户无权限
        mockMvc.perform(multipart("/api/file/uploadTemplate")
                .file(wordFile)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}