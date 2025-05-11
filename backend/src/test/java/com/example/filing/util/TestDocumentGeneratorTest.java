package com.example.filing.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * 测试文档生成器的测试类
 */
class TestDocumentGeneratorTest {

        @Test
        void testCreateBasicFormDocument() throws IOException {
                // 生成基本表单文档
                byte[] docxData = TestDocumentGenerator.createBasicFormDocument();

                // 验证文档内容
                try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxData))) {
                        assertNotNull(document);
                        assertTrue(document.getParagraphs().size() > 0, "文档应包含段落");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("基本信息表")), "应包含标题");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("${name}")), "应包含姓名字段");
                }
        }

        @Test
        void testCreateTableFormDocument() throws IOException {
                // 生成表格表单文档
                byte[] docxData = TestDocumentGenerator.createTableFormDocument();

                // 验证文档内容
                try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxData))) {
                        assertNotNull(document);
                        assertTrue(document.getTables().size() > 0, "文档应包含表格");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("项目申请表")), "应包含标题");
                        assertTrue(document.getTables().get(0).getRow(0).getCell(0).getText()
                                        .contains("项目名称"), "应包含项目名称表头");
                }
        }

        @Test
        void testCreateMixedFieldsDocument() throws IOException {
                // 生成混合字段文档
                byte[] docxData = TestDocumentGenerator.createMixedFieldsDocument();

                // 验证文档内容
                try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxData))) {
                        assertNotNull(document);
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("综合信息表")), "应包含标题");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("${work_experience}")), "应包含工作经历字段");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("技能特长")), "应包含技能特长部分");
                }
        }

        @Test
        void testCreateDocumentWithCurrentDate() throws IOException {
                // 生成日期测试文档
                byte[] docxData = TestDocumentGenerator.createDocumentWithCurrentDate();

                // 验证文档内容
                try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxData))) {
                        assertNotNull(document);
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("日期测试文档")), "应包含标题");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("${start_date}")), "应包含开始日期字段");
                        assertTrue(document.getParagraphs().stream()
                                        .anyMatch(p -> p.getText().contains("${end_date}")), "应包含结束日期字段");
                }
        }
}