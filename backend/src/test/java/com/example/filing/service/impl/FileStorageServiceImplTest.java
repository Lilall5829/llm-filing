package com.example.filing.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.service.FileStorageService;

public class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private MultipartFile testFile;

    @BeforeEach
    public void setup() {
        fileStorageService = new FileStorageServiceImpl(tempDir);

        // 创建测试文件
        testFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes());
    }

    @Test
    public void testStoreFile() {
        // 测试存储文件
        String fileName = fileStorageService.storeFile(testFile, "test");

        // 验证文件名不为空且生成了UUID格式的文件名
        assertNotNull(fileName);
        assertTrue(fileName.endsWith(".txt"));

        // 验证文件确实存储在目标位置
        Path targetPath = tempDir.resolve(Paths.get("test", fileName));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    public void testLoadFileAsResource() throws IOException {
        // 先存储文件
        String storedFileName = fileStorageService.storeFile(testFile, "test");

        // 然后加载文件
        Resource resource = fileStorageService.loadFileAsResource(storedFileName, "test");

        // 验证资源存在且内容正确
        assertTrue(resource.exists());
        assertEquals("Hello, World!", new String(Files.readAllBytes(resource.getFile().toPath())));
    }

    @Test
    public void testGetFilePath() {
        // 测试获取文件路径
        String fileName = "test.txt";
        Path filePath = fileStorageService.getFilePath(fileName, "test");

        // 验证路径正确
        assertEquals(tempDir.resolve(Paths.get("test", fileName)), filePath);
    }

    @Test
    public void testDeleteFile() throws IOException {
        // 先存储文件
        String storedFileName = fileStorageService.storeFile(testFile, "test");
        Path filePath = tempDir.resolve(Paths.get("test", storedFileName));

        // 验证文件存在
        assertTrue(Files.exists(filePath));

        // 测试删除文件
        boolean deleted = fileStorageService.deleteFile(storedFileName, "test");

        // 验证删除成功且文件不再存在
        assertTrue(deleted);
        assertFalse(Files.exists(filePath));
    }

    @Test
    public void testStoreFileWithInvalidName() {
        // 创建带有无效文件名的测试文件
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "../test.txt",
                "text/plain",
                "Invalid file".getBytes());

        // 验证异常抛出
        assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(invalidFile, "test");
        });
    }

    @Test
    public void testLoadNonExistentFile() {
        // 测试加载不存在的文件
        assertThrows(RuntimeException.class, () -> {
            fileStorageService.loadFileAsResource("non-existent.txt", "test");
        });
    }
}