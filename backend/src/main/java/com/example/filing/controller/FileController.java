package com.example.filing.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.service.FileStorageService;
import com.example.filing.service.WordDocumentService;
import com.example.filing.util.Result;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileStorageService fileStorageService;
    private final WordDocumentService wordDocumentService;

    @Autowired
    public FileController(FileStorageService fileStorageService, WordDocumentService wordDocumentService) {
        this.fileStorageService = fileStorageService;
        this.wordDocumentService = wordDocumentService;
    }

    /**
     * 上传Word模板文件
     * 
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping(value = "/uploadTemplate", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Result<Map<String, String>>> uploadTemplate(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.failed("Please select a file to upload"));
        }

        // 验证文件是否为Word文档
        if (!wordDocumentService.isValidWordDocument(file)) {
            return ResponseEntity.badRequest().body(Result.failed("Only Word documents (.docx) are allowed"));
        }

        try {
            // 保存文件
            String fileName = fileStorageService.storeFile(file, "templates");

            // 解析Word文档
            String templateContent = wordDocumentService.parseWordDocument(file);

            Map<String, String> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("templateContent", templateContent);

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.failed("Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * 下载文件
     * 
     * @param fileName     文件名
     * @param subDirectory 子目录
     * @return 文件资源
     */
    @GetMapping("/download/{subDirectory}/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, @PathVariable String subDirectory) {
        try {
            // 加载文件为资源
            Resource resource = fileStorageService.loadFileAsResource(fileName, subDirectory);

            // 检查资源是否存在
            if (!resource.exists()) {
                throw new RuntimeException("File not found: " + fileName);
            }

            // 确定内容类型
            String contentType = "application/octet-stream";
            String filename = resource.getFilename() != null ? resource.getFilename() : fileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     * 
     * @param fileName     文件名
     * @param subDirectory 子目录
     * @return 删除结果
     */
    @DeleteMapping(value = "/delete/{subDirectory}/{fileName:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Result<String>> deleteFile(@PathVariable String fileName, @PathVariable String subDirectory) {
        boolean deleted = fileStorageService.deleteFile(fileName, subDirectory);
        if (deleted) {
            return ResponseEntity.ok(Result.success("File deleted successfully"));
        } else {
            return ResponseEntity.badRequest().body(Result.failed("Failed to delete file"));
        }
    }
}