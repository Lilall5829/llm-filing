package com.example.filing.service;

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
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.filing.service.impl.FileStorageServiceImpl;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // 设置存储路径为临时目录
        fileStorageService = new FileStorageServiceImpl(tempDir);

        // 创建测试文件
        String content = "测试文件内容";
        multipartFile = new MockMultipartFile(
                "testfile",
                "test.txt",
                "text/plain",
                content.getBytes());
    }

    @Test
    void testStoreFile() {
        // 存储文件
        String fileName = fileStorageService.storeFile(multipartFile, "testDir");

        // 验证文件名不为空且有.txt后缀
        assertNotNull(fileName);
        assertTrue(fileName.endsWith(".txt"));

        // 验证文件是否存在
        Path filePath = tempDir.resolve(Paths.get("testDir", fileName));
        assertTrue(Files.exists(filePath));
    }

    @Test
    void testStoreFileWithInvalidName() {
        // 创建带有无效路径字符的文件
        MockMultipartFile invalidFile = new MockMultipartFile(
                "testfile",
                "../test.txt",
                "text/plain",
                "测试内容".getBytes());

        // 验证抛出异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(invalidFile, "testDir");
        });

        assertTrue(exception.getMessage().contains("invalid path sequence"));
    }

    @Test
    void testLoadFileAsResource() throws IOException {
        // 先存储文件
        String fileName = fileStorageService.storeFile(multipartFile, "testDir");

        // 加载文件
        Resource resource = fileStorageService.loadFileAsResource(fileName, "testDir");

        // 验证资源
        assertTrue(resource.exists());
        assertEquals(fileName, resource.getFilename());
    }

    @Test
    void testLoadNonExistentFile() {
        // 验证加载不存在的文件时抛出异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.loadFileAsResource("non-existent.txt", "testDir");
        });

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    void testGetFilePath() {
        // 获取文件路径
        Path filePath = fileStorageService.getFilePath("test.txt", "testDir");

        // 验证路径
        assertEquals(tempDir.resolve(Paths.get("testDir", "test.txt")), filePath);
    }

    @Test
    void testDeleteFile() throws IOException {
        // 先存储文件
        String fileName = fileStorageService.storeFile(multipartFile, "testDir");
        Path filePath = tempDir.resolve(Paths.get("testDir", fileName));

        // 验证文件存在
        assertTrue(Files.exists(filePath));

        // 删除文件
        boolean result = fileStorageService.deleteFile(fileName, "testDir");

        // 验证删除结果
        assertTrue(result);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testDeleteNonExistentFile() {
        // 删除不存在的文件
        boolean result = fileStorageService.deleteFile("non-existent.txt", "testDir");

        // 验证结果（应该返回false，不抛出异常）
        assertFalse(result);
    }
}