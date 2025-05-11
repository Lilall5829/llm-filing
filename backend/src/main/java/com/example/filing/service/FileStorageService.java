package com.example.filing.service;

import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * 保存上传的文件
     * 
     * @param file         上传的文件
     * @param subDirectory 子目录
     * @return 保存的文件名
     */
    String storeFile(MultipartFile file, String subDirectory);

    /**
     * 加载文件为资源
     * 
     * @param fileName     文件名
     * @param subDirectory 子目录
     * @return 资源对象
     */
    Resource loadFileAsResource(String fileName, String subDirectory);

    /**
     * 获取文件的保存路径
     * 
     * @param fileName     文件名
     * @param subDirectory 子目录
     * @return 文件路径
     */
    Path getFilePath(String fileName, String subDirectory);

    /**
     * 删除文件
     * 
     * @param fileName     文件名
     * @param subDirectory 子目录
     * @return 是否删除成功
     */
    boolean deleteFile(String fileName, String subDirectory);
}