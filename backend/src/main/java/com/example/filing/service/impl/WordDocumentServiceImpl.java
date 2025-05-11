package com.example.filing.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.service.WordDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WordDocumentServiceImpl implements WordDocumentService {

    private final ObjectMapper objectMapper;

    public WordDocumentServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String parseWordDocument(MultipartFile file) {
        try {
            return parseWordDocument(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Word document", e);
        }
    }

    @Override
    public String parseWordDocument(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Object> sections = new LinkedHashMap<>();

            // 解析段落
            int paragraphIndex = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    // 根据段落样式分析是否是标题
                    if (isHeading(paragraph)) {
                        String headingId = "heading_" + paragraphIndex;
                        Map<String, Object> headingInfo = new HashMap<>();
                        headingInfo.put("text", text);
                        headingInfo.put("level", getHeadingLevel(paragraph));
                        sections.put(headingId, headingInfo);
                    } else {
                        String paragraphId = "paragraph_" + paragraphIndex;
                        sections.put(paragraphId, text);
                    }
                }
                paragraphIndex++;
            }

            // 解析表格
            int tableIndex = 0;
            for (XWPFTable table : document.getTables()) {
                String tableId = "table_" + tableIndex;
                List<Map<String, String>> tableData = parseTable(table);
                sections.put(tableId, tableData);
                tableIndex++;
            }

            result.put("sections", sections);

            // 将结果转换为JSON字符串
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Word document", e);
        }
    }

    @Override
    public boolean isValidWordDocument(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        // 检查文件扩展名
        String lowerCaseFilename = originalFilename.toLowerCase();
        if (!lowerCaseFilename.endsWith(".docx") && !lowerCaseFilename.endsWith(".doc")) {
            return false;
        }

        // 尝试打开文件以验证格式
        try (InputStream is = file.getInputStream()) {
            if (lowerCaseFilename.endsWith(".docx")) {
                new XWPFDocument(is);
            } else {
                // 如果需要支持旧版.doc格式，需要添加额外的处理
                // 当前仅支持.docx格式
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHeading(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        return style != null && style.startsWith("Heading");
    }

    private int getHeadingLevel(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        if (style != null && style.startsWith("Heading")) {
            try {
                return Integer.parseInt(style.substring("Heading".length()).trim());
            } catch (NumberFormatException e) {
                // 默认为1级标题
                return 1;
            }
        }
        return 1;
    }

    private List<Map<String, String>> parseTable(XWPFTable table) {
        List<Map<String, String>> tableData = new ArrayList<>();

        // 获取表头（如果存在）
        List<String> headers = new ArrayList<>();
        if (!table.getRows().isEmpty()) {
            XWPFTableRow headerRow = table.getRow(0);
            for (XWPFTableCell cell : headerRow.getTableCells()) {
                headers.add(cell.getText());
            }
        }

        // 从第二行开始解析数据（假设第一行是表头）
        for (int i = 1; i < table.getNumberOfRows(); i++) {
            XWPFTableRow row = table.getRow(i);
            Map<String, String> rowData = new LinkedHashMap<>();

            List<XWPFTableCell> cells = row.getTableCells();
            for (int j = 0; j < cells.size(); j++) {
                String header = j < headers.size() ? headers.get(j) : "Column" + (j + 1);
                rowData.put(header, cells.get(j).getText());
            }

            tableData.add(rowData);
        }

        return tableData;
    }
}