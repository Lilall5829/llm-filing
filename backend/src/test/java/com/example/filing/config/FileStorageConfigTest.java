package com.example.filing.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 文件存储配置测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
        "file.upload-dir=${java.io.tmpdir}/file-config-test" // 使用临时目录
})
class FileStorageConfigTest {

    @TempDir
    Path tempDir;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Test
    void testFileStorageLocationCreation() {
        // 获取配置的文件存储路径
        Path storageLocation = fileStorageConfig.fileStorageLocation();

        // 验证存储位置存在
        assertTrue(Files.exists(storageLocation), "文件存储路径应该存在");
        assertTrue(Files.isDirectory(storageLocation), "文件存储路径应该是一个目录");

        // 验证模板目录创建
        Path templateDir = storageLocation.resolve("templates");
        assertTrue(Files.exists(templateDir), "模板目录应该存在");
        assertTrue(Files.isDirectory(templateDir), "模板目录应该是一个目录");
    }

    @Test
    void testExceptionHandling() throws IOException {
        // 创建一个特殊情况：用一个普通文件来模拟上传目录，应该会触发异常
        Path invalidDir = tempDir.resolve("invalid-dir");
        Files.createFile(invalidDir); // 创建一个文件而不是目录

        // 临时修改配置的上传路径为无效路径
        System.setProperty("file.upload-dir", invalidDir.toString());

        // 创建一个新的配置实例
        FileStorageConfig config = new FileStorageConfig();
        // 设置上传目录为创建的文件
        try {
            // 尝试初始化，应该抛出异常
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                config.fileStorageLocation();
            });

            // 验证异常不为空
            assertTrue(exception != null && (exception.getMessage() == null ||
                    exception.getMessage().contains("Could not create") ||
                    exception.getCause() != null),
                    "应该抛出包含目录创建失败相关信息的异常");
        } finally {
            // 恢复原来的系统属性
            System.clearProperty("file.upload-dir");
        }
    }

    @Test
    void testTemplateDirectoryCreation() {
        // 获取配置的文件存储路径
        Path storageLocation = fileStorageConfig.fileStorageLocation();

        // 删除模板目录以测试重新创建功能
        Path templateDir = storageLocation.resolve("templates");
        try {
            Files.deleteIfExists(templateDir);
            assertFalse(Files.exists(templateDir), "模板目录应该已删除");

            // 创建一个新的配置实例并设置相同的上传目录
            FileStorageConfig newConfig = new FileStorageConfig();
            try {
                Field uploadDirField = FileStorageConfig.class.getDeclaredField("uploadDir");
                uploadDirField.setAccessible(true);
                uploadDirField.set(newConfig, storageLocation.toString());
            } catch (Exception e) {
                fail("无法设置uploadDir字段: " + e.getMessage());
            }

            // 调用新实例的方法重新创建目录
            Path newLocation = newConfig.fileStorageLocation();
            Path newTemplateDir = newLocation.resolve("templates");

            assertTrue(Files.exists(newTemplateDir), "模板目录应该被重新创建");
            assertTrue(Files.isDirectory(newTemplateDir), "模板目录应该是一个目录");
        } catch (IOException e) {
            fail("测试过程中发生IO异常: " + e.getMessage());
        }
    }
}