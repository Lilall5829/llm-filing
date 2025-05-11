package com.example.filing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.filing.service.WordDocumentService;
import com.example.filing.util.TestDocumentGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 模板内容结构化处理的集成测试
 * 测试从上传Word文档到结构化解析的完整流程
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TemplateContentParsingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WordDocumentService wordDocumentService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // Create test documents for testing
        try {
            // Ensure any test prerequisites are properly set up
            // Generate test document
            byte[] docxContent = TestDocumentGenerator.createBasicFormDocument();
            if (docxContent != null && docxContent.length > 0) {
                // We have valid test data, ensure proper setup for tests
                MockMultipartFile sampleFile = new MockMultipartFile(
                        "file",
                        "basic-form.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        docxContent);

                // Pre-validate this file to ensure later tests work properly
                if (wordDocumentService != null) {
                    // This call helps initialize the service properly for tests
                    boolean isValid = wordDocumentService.isValidWordDocument(sampleFile);
                    System.out.println("WordDocumentService test initialization: File valid = " + isValid);
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Test setup encountered an issue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试预览基本表单文档
     */
    @Test
    @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
    public void testPreviewBasicFormDocument() throws Exception {
        // 创建基本表单文档
        byte[] docxContent = TestDocumentGenerator.createBasicFormDocument();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "basic-form.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent);

        // 执行预览请求
        MvcResult result = mockMvc.perform(multipart("/api/templateRegistry/previewTemplateContent")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        // 验证响应
        validatePreviewResponse(result);
    }

    /**
     * 测试预览表格表单文档
     */
    @Test
    @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
    public void testPreviewTableFormDocument() throws Exception {
        // 创建表格表单文档
        byte[] docxContent = TestDocumentGenerator.createTableFormDocument();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "table-form.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent);

        // 执行预览请求
        MvcResult result = mockMvc.perform(multipart("/api/templateRegistry/previewTemplateContent")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        // 验证响应
        validatePreviewResponse(result);
    }

    /**
     * 测试预览混合字段文档
     */
    @Test
    @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
    public void testPreviewMixedFieldsDocument() throws Exception {
        // 创建混合字段文档
        byte[] docxContent = TestDocumentGenerator.createMixedFieldsDocument();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mixed-fields.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent);

        // 执行预览请求
        MvcResult result = mockMvc.perform(multipart("/api/templateRegistry/previewTemplateContent")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        // 验证响应
        validatePreviewResponse(result);
    }

    /**
     * 测试直接解析Word文档
     */
    @Test
    public void testDirectParseWordDocument() throws Exception {
        // 创建混合字段文档
        byte[] docxContent = TestDocumentGenerator.createMixedFieldsDocument();

        // 直接使用服务解析文档
        String templateContent = wordDocumentService.parseWordDocument(
                new ByteArrayInputStream(docxContent));

        // 验证解析结果
        JsonNode jsonContent = objectMapper.readTree(templateContent);

        // 检查关键结构
        assertNotNull(jsonContent.get("sections"));
        assertNotNull(jsonContent.get("formFields"));
        assertNotNull(jsonContent.get("metadata"));

        // 验证表单字段
        JsonNode formFields = jsonContent.get("formFields");
        assertTrue(formFields.isArray());
        assertTrue(formFields.size() > 5, "应至少解析出5个表单字段");

        // 验证表单字段类型
        boolean hasTextField = false;
        boolean hasDateField = false;
        boolean hasTextareaField = false;

        for (JsonNode field : formFields) {
            String type = field.get("type").asText();
            if ("text".equals(type))
                hasTextField = true;
            if ("date".equals(type))
                hasDateField = true;
            if ("textarea".equals(type))
                hasTextareaField = true;
        }

        assertTrue(hasTextField, "应识别出文本类型字段");
        assertTrue(hasDateField, "应识别出日期类型字段");
        assertTrue(hasTextareaField, "应识别出多行文本类型字段");
    }

    /**
     * 验证预览响应
     */
    private void validatePreviewResponse(MvcResult result) throws Exception {
        // 解析结果
        String responseContent = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // 验证响应基本结构
        assertTrue(responseJson.has("code"));
        assertEquals(200, responseJson.get("code").asInt());
        assertTrue(responseJson.has("data"));

        // 验证解析的模板内容
        JsonNode dataNode = responseJson.get("data");
        assertTrue(dataNode.has("templateContent"));
        assertTrue(dataNode.has("statistics"));

        // 验证统计信息
        JsonNode statsNode = dataNode.get("statistics");
        assertTrue(statsNode.has("totalFields"));
        assertTrue(statsNode.has("fieldTypeCount"));

        // 验证模板内容
        JsonNode templateContent = dataNode.get("templateContent");
        assertTrue(templateContent.has("sections"));
        assertTrue(templateContent.has("formFields"));
        assertTrue(templateContent.has("metadata"));

        // 验证表单字段列表
        JsonNode formFields = templateContent.get("formFields");
        assertTrue(formFields.isArray());
        assertTrue(formFields.size() > 0, "应至少解析出一个表单字段");
    }
}