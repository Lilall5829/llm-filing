package com.example.filing.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.example.filing.service.WordDocumentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WordDocumentServiceImplTest {

    @TempDir
    Path tempDir;

    private WordDocumentService wordDocumentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        wordDocumentService = new WordDocumentServiceImpl(objectMapper);
    }

    @Test
    public void testIsValidWordDocument() throws IOException {
        // 创建一个有效的DOCX文件
        Path docxPath = createSimpleDocxFile();
        byte[] docxContent = Files.readAllBytes(docxPath);

        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent);

        // 测试有效文件
        assertTrue(wordDocumentService.isValidWordDocument(validFile));

        // 测试无效文件
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is not a Word document".getBytes());

        assertFalse(wordDocumentService.isValidWordDocument(invalidFile));

        // 测试扩展名不匹配的文件
        MockMultipartFile wrongExtensionFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                docxContent);

        assertFalse(wordDocumentService.isValidWordDocument(wrongExtensionFile));
    }

    @Test
    public void testParseWordDocument() throws Exception {
        // 创建一个带有文本内容的Word文档
        Path docxPath = createSimpleDocxFile();
        byte[] docxContent = Files.readAllBytes(docxPath);

        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent);

        // 解析文档
        String result = wordDocumentService.parseWordDocument(docxFile);

        // 验证结果是有效的JSON且包含预期内容
        assertNotNull(result);
        JsonNode rootNode = objectMapper.readTree(result);

        // 打印实际的JSON结构以便调试
        System.out.println("Actual JSON structure:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));

        // 验证基本结构
        assertTrue(rootNode.has("sections"), "应包含sections字段");
        assertTrue(rootNode.has("formFields"), "应包含formFields字段");
        assertTrue(rootNode.has("metadata"), "应包含metadata字段");

        // 验证sections内容
        JsonNode sections = rootNode.get("sections");
        assertTrue(sections.isObject(), "sections应为对象");
        assertTrue(sections.size() > 0, "sections不应为空");

        // 验证文本内容
        boolean foundTestText = false;
        for (JsonNode section : sections) {
            if (section.has("text")) {
                String text = section.get("text").asText();
                if (text.contains("This is a test document")) {
                    foundTestText = true;
                    break;
                }
            }
        }
        assertTrue(foundTestText, "解析结果中应包含测试文本");
    }

    @Test
    public void testParseWordDocumentInputStream() throws Exception {
        // 创建一个带有文本内容的Word文档
        Path docxPath = createSimpleDocxFile();
        byte[] docxContent = Files.readAllBytes(docxPath);

        // 使用输入流解析
        InputStream inputStream = new ByteArrayInputStream(docxContent);
        String result = wordDocumentService.parseWordDocument(inputStream);

        // 验证结果是有效的JSON且包含预期内容
        assertNotNull(result);
        JsonNode rootNode = objectMapper.readTree(result);
        assertTrue(rootNode.has("sections"));
    }

    @Test
    public void testParseInvalidWordDocument() {
        // 创建无效的文档文件
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "This is not a valid DOCX file".getBytes());

        // 验证解析无效文档时抛出异常
        assertThrows(RuntimeException.class, () -> {
            wordDocumentService.parseWordDocument(invalidFile);
        });
    }

    /**
     * 创建一个简单的DOCX文件用于测试
     */
    private Path createSimpleDocxFile() throws IOException {
        // 创建一个临时文件
        Path tempFile = tempDir.resolve("test.docx");

        // 创建一个Word文档
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加段落
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("This is a test document for WordDocumentService");

            // 保存到临时文件
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile.toFile())) {
                document.write(out);
            }
        }

        return tempFile;
    }
}