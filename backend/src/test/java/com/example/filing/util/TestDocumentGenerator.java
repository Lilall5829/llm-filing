package com.example.filing.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * 测试文档生成器
 * 用于生成各种测试场景的Word文档样本
 */
public class TestDocumentGenerator {

    /**
     * 生成一个包含基本表单字段的测试文档
     */
    public static byte[] createBasicFormDocument() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Heading1");
            title.createRun().setText("基本信息表");

            // 添加说明文字
            XWPFParagraph description = document.createParagraph();
            description.createRun().setText("请填写以下基本信息：");

            // 添加基本字段
            addFieldParagraph(document, "姓名", "${name}");
            addFieldParagraph(document, "性别", "${gender}");
            addFieldParagraph(document, "出生日期", "${birth_date}");
            addFieldParagraph(document, "身份证号", "${id_number}");
            addFieldParagraph(document, "联系电话", "${phone}");
            addFieldParagraph(document, "电子邮箱", "${email}");
            addFieldParagraph(document, "现居地址", "${address}");

            return convertToBytes(document);
        }
    }

    /**
     * 生成一个包含复杂表格的测试文档
     */
    public static byte[] createTableFormDocument() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Heading1");
            title.createRun().setText("项目申请表");

            // 添加基本信息表格
            XWPFTable basicInfoTable = document.createTable(4, 2);
            basicInfoTable.getRow(0).getCell(0).setText("项目名称");
            basicInfoTable.getRow(0).getCell(1).setText("${project_name}");
            basicInfoTable.getRow(1).getCell(0).setText("申请日期");
            basicInfoTable.getRow(1).getCell(1).setText("${apply_date}");
            basicInfoTable.getRow(2).getCell(0).setText("负责人");
            basicInfoTable.getRow(2).getCell(1).setText("${project_leader}");
            basicInfoTable.getRow(3).getCell(0).setText("联系电话");
            basicInfoTable.getRow(3).getCell(1).setText("${contact_phone}");

            // 添加项目成员表格
            XWPFParagraph membersTitle = document.createParagraph();
            membersTitle.createRun().setText("项目成员信息：");

            XWPFTable membersTable = document.createTable(4, 4);
            // 设置表头
            XWPFTableRow headerRow = membersTable.getRow(0);
            headerRow.getCell(0).setText("序号");
            headerRow.getCell(1).setText("姓名");
            headerRow.getCell(2).setText("职务");
            headerRow.getCell(3).setText("联系方式");

            // 设置数据行
            for (int i = 1; i < 4; i++) {
                XWPFTableRow row = membersTable.getRow(i);
                row.getCell(0).setText(String.valueOf(i));
                row.getCell(1).setText("${member_" + i + "_name}");
                row.getCell(2).setText("${member_" + i + "_position}");
                row.getCell(3).setText("${member_" + i + "_contact}");
            }

            return convertToBytes(document);
        }
    }

    /**
     * 生成一个包含各种类型字段的测试文档
     */
    public static byte[] createMixedFieldsDocument() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Heading1");
            title.createRun().setText("综合信息表");

            // 添加文本字段
            addFieldParagraph(document, "姓名", "${name}");
            addFieldParagraph(document, "工号", "${employee_id}");

            // 添加日期字段
            addFieldParagraph(document, "入职日期", "${hire_date}");
            addFieldParagraph(document, "合同到期日", "${contract_end_date}");

            // 添加多行文本
            XWPFParagraph textAreaTitle = document.createParagraph();
            textAreaTitle.createRun().setText("工作经历：");
            XWPFParagraph textArea = document.createParagraph();
            textArea.createRun().setText("${work_experience}");

            // 添加单选选项
            XWPFParagraph radioTitle = document.createParagraph();
            radioTitle.createRun().setText("婚姻状况：");
            XWPFParagraph radioOptions = document.createParagraph();
            radioOptions.createRun().setText(
                    "○ 未婚 ${marital_status_single}  ○ 已婚 ${marital_status_married}  ○ 离异 ${marital_status_divorced}");

            // 添加多选选项
            XWPFParagraph checkboxTitle = document.createParagraph();
            checkboxTitle.createRun().setText("技能特长（可多选）：");
            XWPFParagraph checkboxOptions = document.createParagraph();
            checkboxOptions.createRun().setText(
                    "□ Java ${skill_java}  □ Python ${skill_python}  □ JavaScript ${skill_js}  □ 数据库 ${skill_db}");

            // 添加下拉选择
            addFieldParagraph(document, "学历", "${education_level}");
            addFieldParagraph(document, "部门", "${department}");

            return convertToBytes(document);
        }
    }

    /**
     * 生成一个包含当前日期的测试文档
     */
    public static byte[] createDocumentWithCurrentDate() throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Heading1");
            title.createRun().setText("日期测试文档");

            // 添加当前日期
            String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            addFieldParagraph(document, "生成日期", currentDate);

            // 添加日期字段
            addFieldParagraph(document, "开始日期", "${start_date}");
            addFieldParagraph(document, "结束日期", "${end_date}");
            addFieldParagraph(document, "计划日期", "${planned_date}");

            return convertToBytes(document);
        }
    }

    /**
     * 辅助方法：添加带有标签和字段的段落
     */
    private static void addFieldParagraph(XWPFDocument document, String label, String fieldPlaceholder) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().setText(label + ": " + fieldPlaceholder);
    }

    /**
     * 辅助方法：将文档转换为字节数组
     */
    private static byte[] convertToBytes(XWPFDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        return out.toByteArray();
    }
}