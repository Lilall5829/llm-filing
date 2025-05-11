package com.example.filing.config;

import java.io.IOException;
import java.util.Collections;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.filing.security.UserDetailsImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 测试环境下的安全配置
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/templateRegistry/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/file/uploadTemplate").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/file/delete/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/file/download/**").permitAll()
                        .requestMatchers("/api/userTemplate/**").authenticated()
                        .requestMatchers("/hello").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new TestAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Authentication filter for tests that creates a UserDetailsImpl principal
     * instead of a standard Spring User object to match controller expectations
     */
    private static class TestAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain)
                throws ServletException, IOException {
            // Only set authentication if not already set by @WithMockUser
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create a UserDetailsImpl instance to match what the controller expects
                UserDetailsImpl userDetails = new UserDetailsImpl(
                        "test-admin-id", // id
                        "Admin User", // username
                        "admin", // loginName
                        "password", // password
                        1, // role (1=admin)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        }
    }
}