package com.example.filing.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * 自定义WithMockUser注解的SecurityContext工厂
 * 使用我们自己的UserDetailsImpl替代默认的User
 */
public class CustomWithMockUserSecurityContextFactory implements WithSecurityContextFactory<WithMockUser> {

    private final UserDetailsService userDetailsService;

    public CustomWithMockUserSecurityContextFactory(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public SecurityContext createSecurityContext(WithMockUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // 使用注解中的username获取UserDetails
        UserDetails principal = userDetailsService.loadUserByUsername(
                annotation.value().isEmpty() ? annotation.username() : annotation.value());

        // 创建认证令牌
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities());

        context.setAuthentication(auth);
        return context;
    }
}