package com.example.filing.config;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * 此配置类用于覆盖TestConfig中的H2数据源配置
 * 当使用testcontainers profile时生效
 */
@TestConfiguration
@Profile("testcontainers")
@Order(1) // 确保比TestConfig优先级高
public class TestConfigOverride {

    /**
     * 覆盖TestConfig中的dataSource方法
     * 返回null，让TestContainersConfig中的dataSource生效
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        // 不做任何事情，让TestContainersConfig中的dataSource生效
        System.out.println("TestConfigOverride: 禁用H2数据源，使用TestContainers MySQL数据源");
        return null;
    }
}