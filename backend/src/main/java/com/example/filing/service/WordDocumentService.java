package com.example.filing.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface WordDocumentService {

    /**
     * 解析Word文档内容
     * 
     * @param file Word文档文件
     * @return 解析后的结构化数据（JSON格式）
     */
    String parseWordDocument(MultipartFile file);

    /**
     * 解析Word文档内容
     * 
     * @param inputStream Word文档输入流
     * @return 解析后的结构化数据（JSON格式）
     */
    String parseWordDocument(InputStream inputStream);

    /**
     * 验证文件是否为有效的Word文档
     * 
     * @param file 上传的文件
     * @return 是否是有效的Word文档
     */
    boolean isValidWordDocument(MultipartFile file);
}