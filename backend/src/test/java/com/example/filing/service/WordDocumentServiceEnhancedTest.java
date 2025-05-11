package com.example.filing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.example.filing.service.impl.WordDocumentServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 增强的Word文档解析服务测试
 * 此测试类专门测试Word文档结构化解析功能，包括表单域提取、字段类型识别等
 */
class WordDocumentServiceEnhancedTest {

    private WordDocumentService wordDocumentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        wordDocumentService = new WordDocumentServiceImpl(objectMapper);
    }

    @Test
    void testParseDocumentWithFormFields() throws Exception {
        // 创建包含表单字段的Word文档
        byte[] docxData = createDocxWithFormFields();
        MockMultipartFile wordFile = new MockMultipartFile(
                "document",
                "test-form-fields.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxData);

        // 调用解析方法
        String result = wordDocumentService.parseWordDocument(wordFile);

        // 验证JSON结构
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });

        // 验证关键结构存在
        assertNotNull(resultMap.get("sections"));
        assertNotNull(resultMap.get("formFields"));
        assertNotNull(resultMap.get("metadata"));

        // 验证表单字段被正确识别
        List<Map<String, Object>> formFields = (List<Map<String, Object>>) resultMap.get("formFields");
        assertTrue(formFields.size() > 0, "应至少识别一个表单字段");

        // 验证至少有一个名称字段被正确识别
        boolean hasNameField = formFields.stream()
                .anyMatch(field -> "name".equals(field.get("name")));
        assertTrue(hasNameField, "应识别出'name'表单字段");

        // 验证至少有一个日期字段被正确识别为date类型
        boolean hasDateField = formFields.stream()
                .anyMatch(field -> field.get("name").toString().contains("date") && "date".equals(field.get("type")));
        assertTrue(hasDateField, "应识别出日期类型字段");
    }

    @Test
    void testParseDocumentWithTableFields() throws Exception {
        // 创建包含表格和表单字段的Word文档
        byte[] docxData = createDocxWithTableFormFields();
        MockMultipartFile wordFile = new MockMultipartFile(
                "document",
                "test-table-fields.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxData);

        // 调用解析方法
        String result = wordDocumentService.parseWordDocument(wordFile);

        // 验证JSON结构
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });

        // 验证表格被正确解析
        Map<String, Object> sections = (Map<String, Object>) resultMap.get("sections");
        boolean hasTable = sections.values().stream()
                .anyMatch(section -> section instanceof Map && "table".equals(((Map<?, ?>) section).get("type")));
        assertTrue(hasTable, "应识别出表格结构");

        // 验证表格中的表单字段被正确提取
        List<Map<String, Object>> formFields = (List<Map<String, Object>>) resultMap.get("formFields");
        boolean hasTableCellField = formFields.stream()
                .anyMatch(field -> "table_cell".equals(field.get("sourceType")));
        assertTrue(hasTableCellField, "应从表格单元格中提取表单字段");

        // 验证元数据
        Map<String, Object> metadata = (Map<String, Object>) resultMap.get("metadata");
        assertEquals("1.0", metadata.get("version"));
        assertNotNull(metadata.get("createTime"));
    }

    @Test
    void testFieldTypeDetection() throws Exception {
        // 创建包含各种类型字段的Word文档
        byte[] docxData = createDocxWithVariousFieldTypes();
        MockMultipartFile wordFile = new MockMultipartFile(
                "document",
                "test-field-types.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxData);

        // 调用解析方法
        String result = wordDocumentService.parseWordDocument(wordFile);

        // 解析结果
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
        });
        List<Map<String, Object>> formFields = (List<Map<String, Object>>) resultMap.get("formFields");

        // 打印实际的表单字段数据以便调试
        System.out.println("Actual form fields:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formFields));

        // 验证各种字段类型被正确识别
        assertFieldTypeExists(formFields, "date", "date_field");
        assertFieldTypeExists(formFields, "select", "select_option");
        assertFieldTypeExists(formFields, "number", "number_value");
        assertFieldTypeExists(formFields, "checkbox", "checkbox_item");
        assertFieldTypeExists(formFields, "select", "radio_option");
        assertFieldTypeExists(formFields, "textarea", "textarea_content");
        assertFieldTypeExists(formFields, "text", "text_input");
    }

    /**
     * 辅助方法：验证指定类型的字段存在
     */
    private void assertFieldTypeExists(List<Map<String, Object>> fields, String expectedType, String namePattern) {
        boolean exists = fields.stream()
                .anyMatch(field -> {
                    String type = (String) field.get("type");
                    String name = (String) field.get("name");
                    System.out.println("Checking field - Type: " + type + ", Name: " + name);
                    return expectedType.equals(type) && name.contains(namePattern);
                });
        assertTrue(exists, "应存在类型为'" + expectedType + "'且名称包含'" + namePattern + "'的字段");
    }

    /**
     * 创建包含表单字段的Word文档
     */
    private byte[] createDocxWithFormFields() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("测试表单");

            // 添加包含表单字段的段落
            XWPFParagraph paragraph1 = document.createParagraph();
            paragraph1.createRun().setText("姓名: ${name}");

            XWPFParagraph paragraph2 = document.createParagraph();
            paragraph2.createRun().setText("日期: ${date}");

            XWPFParagraph paragraph3 = document.createParagraph();
            paragraph3.createRun().setText("地址: {address}");

            // 将文档转换为字节数组
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建包含表格和表单字段的Word文档
     */
    private byte[] createDocxWithTableFormFields() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("表格测试");

            // 添加表格
            XWPFTable table = document.createTable(3, 3);

            // 设置表头
            XWPFTableRow headerRow = table.getRow(0);
            headerRow.getCell(0).setText("项目");
            headerRow.getCell(1).setText("${header_field}");
            headerRow.getCell(2).setText("备注");

            // 设置数据行
            XWPFTableRow dataRow1 = table.getRow(1);
            dataRow1.getCell(0).setText("项目1");
            dataRow1.getCell(1).setText("${table_field1}");
            dataRow1.getCell(2).setText("备注1");

            XWPFTableRow dataRow2 = table.getRow(2);
            dataRow2.getCell(0).setText("项目2");
            dataRow2.getCell(1).setText("${table_field2}");
            dataRow2.getCell(2).setText("{table_remark}");

            // 将文档转换为字节数组
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建包含各种类型字段的Word文档
     */
    private byte[] createDocxWithVariousFieldTypes() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("字段类型测试");

            // 添加各种类型的字段
            addParagraphWithField(document, "日期字段: ${date_field}");
            addParagraphWithField(document, "下拉选择: ${select_option}");
            addParagraphWithField(document, "数字字段: ${number_value}");
            addParagraphWithField(document, "复选框: ${checkbox_item}");
            addParagraphWithField(document, "单选按钮: ${radio_option}");
            addParagraphWithField(document, "多行文本: ${textarea_content}");
            addParagraphWithField(document, "普通文本: ${text_input}");

            // 将文档转换为字节数组
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 辅助方法：添加包含指定文本的段落
     */
    private void addParagraphWithField(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().setText(text);
    }
}