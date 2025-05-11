package com.example.filing.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 文件存储测试配置类
 */
@TestConfiguration
public class FileStorageTestConfig {

    /**
     * 配置用于测试的文件存储位置
     * 使用临时目录
     */
    @Bean
    @Primary
    public Path fileStorageLocation() throws IOException {
        Path tempDir = Files.createTempDirectory("file-storage-test");
        System.out.println("测试文件存储位置: " + tempDir);

        // 创建模板目录
        Path templateDir = Paths.get(tempDir.toString(), "templates");
        Files.createDirectories(templateDir);

        // 确保测试完成后清理
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.toString().compareTo(a.toString())) // 倒序，先删除内容
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                System.err.println("清理测试文件时出错: " + e.getMessage());
                            }
                        });
            } catch (IOException e) {
                System.err.println("清理测试目录时出错: " + e.getMessage());
            }
        }));

        return tempDir;
    }
}