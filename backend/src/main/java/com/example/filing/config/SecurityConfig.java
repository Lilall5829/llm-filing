package com.example.filing.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.filing.security.JwtAuthenticationFilter;
import com.example.filing.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 安全配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/static/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/templateRegistry/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/file/uploadTemplate").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/file/delete/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/file/download/**").permitAll()
                        .requestMatchers("/api/userTemplate/**").authenticated()
                        .requestMatchers("/hello").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Result<?> result = Result.failed(HttpStatus.FORBIDDEN.value(), "Access Denied");
                            response.getWriter().write(objectMapper.writeValueAsString(result));
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Result<?> result = Result.failed(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
                            response.getWriter().write(objectMapper.writeValueAsString(result));
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}