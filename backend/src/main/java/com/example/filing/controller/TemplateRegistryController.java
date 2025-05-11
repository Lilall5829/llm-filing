package com.example.filing.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.dto.request.TemplateRegistryRequest;
import com.example.filing.security.UserDetailsImpl;
import com.example.filing.service.FileStorageService;
import com.example.filing.service.TemplateRegistryService;
import com.example.filing.service.WordDocumentService;
import com.example.filing.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 模板管理控制器
 */
@RestController
@RequestMapping("/api/templateRegistry")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class TemplateRegistryController {

    private final TemplateRegistryService templateRegistryService;
    private final FileStorageService fileStorageService;
    private final WordDocumentService wordDocumentService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TemplateRegistryController(TemplateRegistryService templateRegistryService,
            FileStorageService fileStorageService,
            WordDocumentService wordDocumentService,
            ObjectMapper objectMapper) {
        this.templateRegistryService = templateRegistryService;
        this.fileStorageService = fileStorageService;
        this.wordDocumentService = wordDocumentService;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询模板列表
     * 
     * @param templateCode 模板编号
     * @param templateName 模板名称
     * @param templateType 模板类型
     * @param current      当前页码
     * @param pageSize     每页大小
     * @return 模板分页数据
     */
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<?>> getTemplatesByPage(
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer pageSize) {

        Result<?> result = templateRegistryService.findTemplatesByPage(
                templateCode, templateName, templateType, current, pageSize);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * 保存或更新模板
     * 
     * @param request        模板请求
     * @param authentication 认证信息
     * @return 保存结果
     */
    @PostMapping(value = "/saveTemplateRegistry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<?>> saveTemplateRegistry(@RequestBody TemplateRegistryRequest request,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Result<?> result = templateRegistryService.saveTemplateRegistry(request, userDetails.getId());

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * 根据ID获取模板详情
     * 
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping(value = "/getTemplateRegistryById", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<?>> getTemplateRegistryById(@RequestParam String id) {
        Result<?> result = templateRegistryService.getTemplateRegistryById(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * 删除模板
     * 
     * @param id 模板ID
     * @return 删除结果
     */
    @DeleteMapping(value = "/deleteTemplate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<?>> deleteTemplate(@RequestParam String id) {
        Result<?> result = templateRegistryService.deleteTemplate(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * 预览解析Word文档内容（不保存到数据库）
     * 
     * @param file 上传的Word文档文件
     * @return 解析结果
     */
    @PostMapping(value = "/previewTemplateContent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<?>> previewTemplateContent(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.failed("请选择文件上传"));
        }

        // 验证文件是否为Word文档
        if (!wordDocumentService.isValidWordDocument(file)) {
            return ResponseEntity.badRequest().body(Result.failed("仅允许上传Word文档(.docx)"));
        }

        try {
            // 解析Word文档
            String templateContent = wordDocumentService.parseWordDocument(file);

            // 转换为对象以便进行字段分析和统计
            Map<String, Object> contentMap = objectMapper.readValue(templateContent, Map.class);

            // 统计表单域数量和类型
            Map<String, Object> stats = new HashMap<>();
            if (contentMap.containsKey("formFields")) {
                stats.put("totalFields", ((java.util.List<?>) contentMap.get("formFields")).size());

                // 按类型统计字段
                Map<String, Integer> fieldTypeCount = new HashMap<>();
                for (Object field : (java.util.List<?>) contentMap.get("formFields")) {
                    Map<?, ?> fieldMap = (Map<?, ?>) field;
                    String type = (String) fieldMap.get("type");
                    fieldTypeCount.put(type, fieldTypeCount.getOrDefault(type, 0) + 1);
                }
                stats.put("fieldTypeCount", fieldTypeCount);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("templateContent", contentMap);
            response.put("statistics", stats);

            return ResponseEntity.ok(Result.success("文档解析成功", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.failed("文档解析失败: " + e.getMessage()));
        }
    }

    /**
     * 上传模板文件并更新模板内容
     * 
     * @param file       上传的Word文档文件
     * @param templateId 模板ID
     * @return 上传结果
     */
    @PostMapping(value = "/uploadTemplateFile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<?>> uploadTemplateFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") String templateId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.failed("请选择文件上传"));
        }

        // 验证文件是否为Word文档
        if (!wordDocumentService.isValidWordDocument(file)) {
            return ResponseEntity.badRequest().body(Result.failed("仅允许上传Word文档(.docx)"));
        }

        try {
            // 保存文件
            String fileName = fileStorageService.storeFile(file, "templates");

            // 解析Word文档
            String templateContent = wordDocumentService.parseWordDocument(file);

            // 转换为对象以便进行基本验证
            Map<String, Object> contentMap = objectMapper.readValue(templateContent, Map.class);

            // 基本验证 - 确保有表单字段
            if (contentMap.containsKey("formFields")) {
                int fieldCount = ((java.util.List<?>) contentMap.get("formFields")).size();
                if (fieldCount == 0) {
                    return ResponseEntity.badRequest().body(Result.failed("未检测到任何表单字段，请检查文档格式"));
                }
            } else {
                return ResponseEntity.badRequest().body(Result.failed("文档结构无效，未包含表单字段定义"));
            }

            // 更新模板文件路径
            String filePath = "templates/" + fileName;
            Result<?> pathUpdateResult = templateRegistryService.updateTemplateFilePath(templateId, filePath);
            if (pathUpdateResult.getCode() != 200) {
                return ResponseEntity.badRequest().body(pathUpdateResult);
            }

            // 更新模板内容
            Result<?> contentUpdateResult = templateRegistryService.updateTemplateContent(templateId, templateContent);
            if (contentUpdateResult.getCode() != 200) {
                return ResponseEntity.badRequest().body(contentUpdateResult);
            }

            // 统计信息
            Map<String, Object> stats = new HashMap<>();
            if (contentMap.containsKey("formFields")) {
                stats.put("totalFields", ((java.util.List<?>) contentMap.get("formFields")).size());

                // 按类型统计字段
                Map<String, Integer> fieldTypeCount = new HashMap<>();
                for (Object field : (java.util.List<?>) contentMap.get("formFields")) {
                    Map<?, ?> fieldMap = (Map<?, ?>) field;
                    String type = (String) fieldMap.get("type");
                    fieldTypeCount.put(type, fieldTypeCount.getOrDefault(type, 0) + 1);
                }
                stats.put("fieldTypeCount", fieldTypeCount);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("filePath", filePath);
            response.put("statistics", stats);

            return ResponseEntity.ok(Result.success("模板文件上传成功，已更新模板内容", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.failed("文件上传失败: " + e.getMessage()));
        }
    }
}