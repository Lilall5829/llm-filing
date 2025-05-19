package com.example.filing.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.dto.request.TemplateRegistryRequest;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.service.impl.FileStorageServiceImpl;
import com.example.filing.service.impl.WordDocumentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class TemplateFileIntegrationTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemplateRegistryRepository templateRegistryRepository;

    private FileStorageService fileStorageService;
    private WordDocumentService wordDocumentService;
    private TemplateRegistryService templateRegistryService;

    private MultipartFile wordFile;
    private TemplateRegistry testTemplateRegistry;
    private String userId = "test-user-id";

    @BeforeEach
    public void setup() throws IOException {
        // 初始化服务
        fileStorageService = new FileStorageServiceImpl(tempDir);
        wordDocumentService = new WordDocumentServiceImpl(objectMapper);

        // 创建测试用Word文档
        Path testDocxPath = createTestDocxFile();
        byte[] fileContent = Files.readAllBytes(testDocxPath);

        wordFile = new MockMultipartFile(
                "file",
                "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                fileContent);

        // 准备测试数据
        testTemplateRegistry = new TemplateRegistry();
        testTemplateRegistry.setId("test-template-id");
        testTemplateRegistry.setTemplateCode("TEST001");
        testTemplateRegistry.setTemplateName("测试模板");
        testTemplateRegistry.setTemplateType("申请表");
        testTemplateRegistry.setDeleted(0);

        // 配置Mock
        when(templateRegistryRepository.save(any(TemplateRegistry.class)))
                .thenReturn(testTemplateRegistry);
        when(templateRegistryRepository.findById(eq("test-template-id")))
                .thenReturn(Optional.of(testTemplateRegistry));
    }

    @Test
    public void testCreateTemplateWithFileUpload() throws IOException {
        // 保存文件
        String fileName = fileStorageService.storeFile(wordFile, "templates");

        // 解析文档内容
        String templateContent = wordDocumentService.parseWordDocument(wordFile);

        // 创建模板请求
        TemplateRegistryRequest request = new TemplateRegistryRequest();
        request.setTemplateName("测试模板");
        request.setTemplateType("申请表");
        request.setTemplateDescription("这是一个集成测试");
        request.setTemplateContent(templateContent);
        request.setFilePath("templates/" + fileName);

        // 验证文件保存成功
        assertTrue(Files.exists(tempDir.resolve(Path.of("templates", fileName))));

        // 验证模板内容有效
        assertNotNull(templateContent);
        assertTrue(templateContent.contains("sections"));

        // 验证模板内容正确解析
        assertTrue(templateContent.contains("This is a test template"));
    }

    @Test
    public void testUpdateTemplateContent() throws IOException {
        // 保存文件
        String fileName = fileStorageService.storeFile(wordFile, "templates");
        String filePath = "templates/" + fileName;

        // 设置模板文件路径
        testTemplateRegistry.setFilePath(filePath);

        // 更新模板内容
        String templateContent = wordDocumentService.parseWordDocument(wordFile);
        testTemplateRegistry.setTemplateContent(templateContent);
        when(templateRegistryRepository.save(any(TemplateRegistry.class)))
                .thenReturn(testTemplateRegistry);

        // 验证文件可以被加载
        assertDoesNotThrow(() -> {
            fileStorageService.loadFileAsResource(fileName, "templates");
        });
    }

    @Test
    public void testDeleteTemplateAndFile() throws IOException {
        // 保存文件
        String fileName = fileStorageService.storeFile(wordFile, "templates");
        String filePath = "templates/" + fileName;

        // 设置模板文件路径
        testTemplateRegistry.setFilePath(filePath);

        // 验证文件存在
        assertTrue(Files.exists(tempDir.resolve(Path.of("templates", fileName))));

        // 删除文件
        boolean deleted = fileStorageService.deleteFile(fileName, "templates");

        // 验证文件删除成功
        assertTrue(deleted);
        assertFalse(Files.exists(tempDir.resolve(Path.of("templates", fileName))));

        // 验证模板逻辑删除
        testTemplateRegistry.setDeleted(1);
        when(templateRegistryRepository.save(any(TemplateRegistry.class)))
                .thenReturn(testTemplateRegistry);

        // 验证模板被标记为已删除
        assertEquals(1, testTemplateRegistry.getDeleted());
    }

    /**
     * 创建测试用Word文档
     */
    private Path createTestDocxFile() throws IOException {
        Path filePath = tempDir.resolve("test.docx");

        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("This is a test template for integration testing");

            try (java.io.FileOutputStream out = new java.io.FileOutputStream(filePath.toFile())) {
                document.write(out);
            }
        }

        return filePath;
    }
}