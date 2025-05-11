package com.example.filing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.filing.security.JwtTokenProvider;

/**
 * 测试环境下的JWT提供者配置
 */
@TestConfiguration
@Profile("test")
public class TestJwtProviderConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", secret);
        ReflectionTestUtils.setField(provider, "expiration", expiration);
        return provider;
    }
}