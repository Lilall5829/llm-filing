package com.example.filing.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Bean
    public Path fileStorageLocation() {
        Path fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where uploaded files will be stored.", ex);
        }

        // 创建模板目录
        Path templateDir = Paths.get(fileStorageLocation.toString(), "templates");
        try {
            Files.createDirectories(templateDir);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create template directory.", ex);
        }

        return fileStorageLocation;
    }
}