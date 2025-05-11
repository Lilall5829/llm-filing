package com.example.filing.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import com.example.filing.service.impl.WordDocumentServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class WordDocumentServiceTest {

    private WordDocumentService wordDocumentService;

    @Mock
    private ObjectMapper objectMapper;

    private MockMultipartFile validWordFile;
    private MockMultipartFile invalidWordFile;
    private MockMultipartFile nonWordFile;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 使用真实的ObjectMapper而不是Mock
        ObjectMapper realObjectMapper = new ObjectMapper();

        // 初始化服务
        wordDocumentService = new WordDocumentServiceImpl(realObjectMapper);

        // 创建有效的Word文档（使用Apache POI创建实际的DOCX文件）
        byte[] docxData = createValidDocxFile();
        validWordFile = new MockMultipartFile(
                "document",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxData);

        // 创建无效的Word文档
        invalidWordFile = new MockMultipartFile(
                "document",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "这不是一个有效的Word文档".getBytes());

        // 创建非Word文档
        nonWordFile = new MockMultipartFile(
                "document",
                "test.txt",
                "text/plain",
                "这是一个文本文件".getBytes());
    }

    /**
     * 创建一个有效的DOCX文件用于测试
     */
    private byte[] createValidDocxFile() throws IOException {
        // 使用Apache POI创建一个简单的Word文档
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加一个段落
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("This is a test document for unit testing");

            // 将文档转换为字节数组
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void testIsValidWordDocument_ValidDocx() throws IOException {
        // 使用内存中创建的有效DOCX文件测试
        assertTrue(wordDocumentService.isValidWordDocument(validWordFile));
    }

    @Test
    void testIsValidWordDocument_InvalidExtension() {
        assertFalse(wordDocumentService.isValidWordDocument(nonWordFile));
    }

    @Test
    void testIsValidWordDocument_NullFilename() {
        MockMultipartFile nullNameFile = new MockMultipartFile(
                "document",
                null,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "content".getBytes());

        assertFalse(wordDocumentService.isValidWordDocument(nullNameFile));
    }

    @Test
    void testParseWordDocument_ValidInput() throws Exception {
        // 使用我们创建的有效文档
        String result = wordDocumentService.parseWordDocument(validWordFile);

        // 验证结果是JSON格式
        JsonNode jsonNode = new ObjectMapper().readTree(result);
        assertNotNull(jsonNode.get("sections"));
    }

    @Test
    void testParseWordDocument_InvalidInput() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            wordDocumentService.parseWordDocument(invalidWordFile);
        });

        assertTrue(exception.getMessage().contains("Failed to parse Word document"));
    }

    @Test
    void testParseWordDocument_InputStreamVersion() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            try (InputStream is = new ByteArrayInputStream("invalid content".getBytes())) {
                wordDocumentService.parseWordDocument(is);
            }
        });

        assertTrue(exception.getMessage().contains("Failed to parse Word document"));
    }
}