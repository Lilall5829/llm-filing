package com.example.filing.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.service.FileStorageService;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageServiceImpl(Path fileStorageLocation) {
        this.fileStorageLocation = fileStorageLocation;
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        // 检查文件名
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // 检查文件名是否包含无效字符
        if (fileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence " + fileName);
        }

        // 生成唯一文件名，保留原始扩展名
        String fileExtension = "";
        if (fileName.contains(".")) {
            fileExtension = fileName.substring(fileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // 创建子目录
        Path targetLocation = createSubDirectoryIfNotExists(subDirectory);

        try {
            // 复制文件到目标位置
            Path filePath = targetLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName, String subDirectory) {
        try {
            Path filePath = getFilePath(fileName, subDirectory);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    @Override
    public Path getFilePath(String fileName, String subDirectory) {
        Path targetLocation = createSubDirectoryIfNotExists(subDirectory);
        return targetLocation.resolve(fileName);
    }

    @Override
    public boolean deleteFile(String fileName, String subDirectory) {
        try {
            Path filePath = getFilePath(fileName, subDirectory);
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }

    private Path createSubDirectoryIfNotExists(String subDirectory) {
        Path targetLocation = this.fileStorageLocation;
        if (subDirectory != null && !subDirectory.isEmpty()) {
            targetLocation = Paths.get(this.fileStorageLocation.toString(), subDirectory);
            try {
                Files.createDirectories(targetLocation);
            } catch (IOException ex) {
                throw new RuntimeException("Could not create directory " + subDirectory, ex);
            }
        }
        return targetLocation;
    }
}