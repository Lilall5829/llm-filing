package com.example.filing.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.example.filing.util.JwtTokenUtil;

/**
 * 测试环境中模拟JWT工具类的配置
 * 使用Mockito创建一个模拟的JwtTokenUtil，避免实际依赖JWT密钥
 */
@TestConfiguration
@Profile("test")
public class TestMockJwtUtil {

    /**
     * 创建一个模拟的JWT工具类，用于测试环境
     * 这样我们就不需要依赖实际的JWT密钥和算法，简化测试
     */
    @Bean
    @Primary
    public JwtTokenUtil mockJwtTokenUtil() {
        JwtTokenUtil mockUtil = mock(JwtTokenUtil.class);

        // 模拟生成管理员令牌
        when(mockUtil.generateToken(anyString(), eq(1))).thenReturn("mock-admin-jwt-token");

        // 模拟生成普通用户令牌
        when(mockUtil.generateToken(anyString(), eq(2))).thenReturn("mock-user-jwt-token");

        // 模拟提取用户名
        when(mockUtil.extractUsername(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if ("mock-admin-jwt-token".equals(token)) {
                return "admin";
            } else if ("mock-user-jwt-token".equals(token)) {
                return "user";
            }
            return null;
        });

        // 模拟提取角色
        when(mockUtil.extractRole(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if ("mock-admin-jwt-token".equals(token)) {
                return 1;
            } else if ("mock-user-jwt-token".equals(token)) {
                return 2;
            }
            return null;
        });

        System.out.println("已创建模拟JWT工具类，用于测试环境");
        return mockUtil;
    }
}