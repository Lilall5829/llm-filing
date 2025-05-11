package com.example.filing.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.filing.controller.TemplateRegistryController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 测试配置类
 */
@TestConfiguration
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(TestSecurityConfig.class)
public class TestConfig implements WebMvcConfigurer {

    @Autowired
    private Environment env;

    /**
     * 配置测试环境的ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 打印序列化调试信息
        try {
            System.out.println("ObjectMapper配置完成，序列化测试: " +
                    mapper.writeValueAsString(com.example.filing.util.Result.success("测试数据")));
        } catch (JsonProcessingException e) {
            System.err.println("序列化测试失败: " + e.getMessage());
        }

        return mapper;
    }

    /**
     * 配置测试环境的MappingJackson2HttpMessageConverter
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());
        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                new MediaType("application", "*+json")));

        System.out.println("配置了MappingJackson2HttpMessageConverter，支持的媒体类型: " +
                converter.getSupportedMediaTypes());

        return converter;
    }

    /**
     * 配置字符串消息转换器
     */
    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_JSON));

        System.out.println("配置了StringHttpMessageConverter，支持的媒体类型: " +
                converter.getSupportedMediaTypes());

        return converter;
    }

    /**
     * 配置资源消息转换器，用于处理Resource类型的响应
     */
    @Bean
    public ResourceHttpMessageConverter resourceHttpMessageConverter() {
        ResourceHttpMessageConverter converter = new ResourceHttpMessageConverter();
        System.out.println("配置了ResourceHttpMessageConverter，支持的媒体类型: " +
                converter.getSupportedMediaTypes());
        return converter;
    }

    /**
     * 配置字节数组消息转换器，用于二进制数据
     */
    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.ALL));
        System.out.println("配置了ByteArrayHttpMessageConverter，支持的媒体类型: " +
                converter.getSupportedMediaTypes());
        return converter;
    }

    /**
     * 确保在测试环境中使用我们配置的消息转换器
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(byteArrayHttpMessageConverter());
        converters.add(resourceHttpMessageConverter());
        converters.add(mappingJackson2HttpMessageConverter());
        converters.add(stringHttpMessageConverter());

        System.out.println("已配置消息转换器，数量: " + converters.size());
    }

    /**
     * 配置内容协商
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("octet-stream", MediaType.APPLICATION_OCTET_STREAM);

        System.out.println("已配置ContentNegotiation，默认内容类型: " + MediaType.APPLICATION_JSON);
    }

    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        System.out.println("已配置静态资源处理");
    }

    /**
     * 配置CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        System.out.println("已配置CORS策略");
    }

    /**
     * 创建一个测试用的数据源，用于测试
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        // 手动初始化数据库，避免使用schema-test.sql中的索引定义
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        builder.setType(EmbeddedDatabaseType.H2);

        // 配置H2数据库的模式为MySQL兼容模式
        builder.addScript("schema-h2.sql");

        // 打印数据源创建信息
        System.out.println("创建H2内存数据库用于测试");

        return builder.build();
    }

    /**
     * 初始化数据库脚本
     */
    @Bean
    public ResourceDatabasePopulator databasePopulator() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema-test.sql"));
        // 如果有需要，可以添加测试数据脚本
        // populator.addScript(new ClassPathResource("data-test.sql"));

        // 设置脚本执行属性
        populator.setContinueOnError(true);
        populator.setSeparator(";");

        return populator;
    }

    /**
     * For tests that use @Import(TestConfig.class), we can provide a custom
     * TemplateRegistryController
     * to avoid authentication issues when using @WithMockUser
     */
    @Bean
    @Primary
    public TemplateRegistryController templateRegistryController() {
        return null; // This will be replaced by the actual implementation in the test classes
    }
}