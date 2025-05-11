package com.example.filing.dto;

/**
 * 登录认证响应记录类
 * 使用Java 17的记录类特性优化DTO
 */
public record AuthenticationResponse(
        String token,
        String userName,
        Integer role) {
}