package com.example.filing.config;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {

    /**
     * 初始化测试数据库
     */
    @Bean
    public DatabaseInitializer databaseInitializer(DataSource dataSource) {
        return new DatabaseInitializer(dataSource);
    }

    public static class DatabaseInitializer {
        public DatabaseInitializer(DataSource dataSource) {
            // 创建数据库初始化工具
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

            // 设置SQL脚本执行顺序 - 使用H2专用脚本
            populator.addScript(new ClassPathResource("schema-h2.sql"));

            // 如果需要添加测试数据，确保data-test.sql存在
            try {
                ClassPathResource dataScript = new ClassPathResource("data-test.sql");
                if (dataScript.exists()) {
                    populator.addScript(dataScript);
                }
            } catch (Exception e) {
                System.out.println("data-test.sql不存在，跳过加载测试数据");
            }

            // 设置脚本执行属性
            populator.setContinueOnError(true); // 允许继续执行，避免非关键错误中断测试
            populator.setSeparator(";"); // 使用分号作为SQL语句分隔符
            populator.setIgnoreFailedDrops(true); // 忽略DROP语句失败

            // 执行初始化脚本
            try {
                System.out.println("正在执行数据库初始化脚本...");
                DatabasePopulatorUtils.execute(populator, dataSource);
                System.out.println("数据库初始化脚本执行完成");
            } catch (Exception e) {
                System.err.println("数据库初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}