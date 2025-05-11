package com.example.filing.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import io.jsonwebtoken.security.Keys;

/**
 * 用于测试的JWT配置
 * 提供一个安全的JWT密钥，确保测试时不会因为密钥长度不足而抛出异常
 */
@TestConfiguration
@Profile("test")
public class TestJwtConfig {

    /**
     * 为测试提供一个安全的JWT密钥
     * 长度至少256位以满足HMAC-SHA256算法的要求
     */
    @Bean
    @Primary
    public SecretKey jwtTestSecretKey() {
        // 提供一个足够长的密钥字符串(至少32个字符，对应256位)
        String secureKeyString = "bhQnUgPYPcwiJMOHOqcfvMlpVbJvGMOeWYoYZGSFrjRPrQoLvBkpzCOLHeKmJTGP";

        // 使用JJWT提供的工具方法创建安全的密钥
        // 这比直接使用字符串更安全，因为它确保密钥具有正确的结构
        SecretKey key = Keys.hmacShaKeyFor(secureKeyString.getBytes(StandardCharsets.UTF_8));

        System.out.println("已创建测试JWT密钥 - 长度: " +
                Base64.getEncoder().encodeToString(key.getEncoded()).length() * 8 + " 位");

        return key;
    }
}