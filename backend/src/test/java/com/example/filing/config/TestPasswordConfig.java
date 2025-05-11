package com.example.filing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 测试环境下的密码编码器配置
 * 用于在测试中绕过密码验证
 */
@Configuration
@Profile("testcontainers")
public class TestPasswordConfig {

    /**
     * 创建一个测试用的PasswordEncoder，它会通过任何密码验证
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                // 直接返回原始密码作为编码后的密码
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                // 在测试环境中，总是返回true，即任何密码都匹配
                System.out.println("测试环境密码验证：" + rawPassword + " 与 " + encodedPassword);
                return true;
            }
        };
    }
}