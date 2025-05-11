package com.example.filing.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import com.zaxxer.hikari.HikariDataSource;

/**
 * TestContainers MySQL配置
 * 使用Docker启动MySQL容器进行测试
 */
@TestConfiguration
@ActiveProfiles("testcontainers")
public class TestContainersConfig {

    // 使用Java 17的文本块定义MySQL版本和命令
    private static final String MYSQL_IMAGE = "mysql:8.0";
    private static final String[] MYSQL_COMMANDS = {
            "--character-set-server=utf8mb4",
            "--collation-server=utf8mb4_unicode_ci"
    };

    // 使用sealed接口和record类优化配置
    private sealed interface DatabaseScript permits InitScript, DataScript {
        String getScriptName();
    }

    private record InitScript(String scriptName) implements DatabaseScript {
        @Override
        public String getScriptName() {
            return scriptName;
        }
    }

    private record DataScript(String scriptName) implements DatabaseScript {
        @Override
        public String getScriptName() {
            return scriptName;
        }
    }

    private static final DatabaseScript SCHEMA_SCRIPT = new InitScript("schema-mysql.sql");
    private static final DatabaseScript DATA_SCRIPT = new DataScript("data-test.sql");

    // 使用静态初始化块管理MySQL容器
    public static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("filing_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand(MYSQL_COMMANDS);

        mysql.start();
        System.out.println("""
                MySQL容器已启动:
                URL: %s
                用户名: %s
                数据库: %s
                """.formatted(
                mysql.getJdbcUrl(),
                mysql.getUsername(),
                mysql.getDatabaseName()));
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(mysql.getJdbcUrl());
        dataSource.setUsername(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        dataSource.setDriverClassName(mysql.getDriverClassName());

        // 初始化数据库
        initDatabase(dataSource);

        return dataSource;
    }

    private void initDatabase(DataSource dataSource) {
        var populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(SCHEMA_SCRIPT.getScriptName()));
        populator.addScript(new ClassPathResource(DATA_SCRIPT.getScriptName()));
        populator.setContinueOnError(true);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(SCHEMA_SCRIPT.getScriptName()));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(DATA_SCRIPT.getScriptName()));
        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
}