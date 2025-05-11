package com.example.filing.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.filing.entity.SysUser;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.util.JwtTokenUtil;

import jakarta.servlet.ServletException;

public class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private SysUserRepository userRepository;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    private SysUser regularUser;
    private SysUser adminUser;
    private SysUser disabledUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Clear security context before each test
        SecurityContextHolder.clearContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        // Setup regular user
        regularUser = new SysUser();
        regularUser.setId("user-id-1");
        regularUser.setLoginName("user");
        regularUser.setPassword("encoded-password");
        regularUser.setUserName("Regular User");
        regularUser.setRole(2);
        regularUser.setStatus(1); // Enabled
        regularUser.setCreateTime(LocalDateTime.now());
        regularUser.setUpdateTime(LocalDateTime.now());

        // Setup admin user
        adminUser = new SysUser();
        adminUser.setId("admin-id-1");
        adminUser.setLoginName("admin");
        adminUser.setPassword("encoded-admin-password");
        adminUser.setUserName("Admin User");
        adminUser.setRole(1);
        adminUser.setStatus(1); // Enabled
        adminUser.setCreateTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());

        // Setup disabled user
        disabledUser = new SysUser();
        disabledUser.setId("disabled-id-1");
        disabledUser.setLoginName("disabled");
        disabledUser.setPassword("encoded-password");
        disabledUser.setUserName("Disabled User");
        disabledUser.setRole(2);
        disabledUser.setStatus(0); // Disabled
        disabledUser.setCreateTime(LocalDateTime.now());
        disabledUser.setUpdateTime(LocalDateTime.now());
    }

    @Test
    public void testDoFilterInternal_NoToken() throws ServletException, IOException {
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtil, never()).extractUsername(anyString());
        verify(userRepository, never()).findByLoginName(anyString());
    }

    @Test
    public void testDoFilterInternal_ValidTokenRegularUser() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenUtil.extractUsername(token)).thenReturn("user");
        when(jwtTokenUtil.extractRole(token)).thenReturn(2);
        when(userRepository.findByLoginName("user")).thenReturn(regularUser);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtil, times(1)).extractUsername(token);
        verify(jwtTokenUtil, times(1)).extractRole(token);
        verify(userRepository, times(1)).findByLoginName("user");
        assert SecurityContextHolder.getContext().getAuthentication() != null;
        assert SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    public void testDoFilterInternal_ValidTokenAdminUser() throws ServletException, IOException {
        // Arrange
        String token = "valid.admin.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenUtil.extractUsername(token)).thenReturn("admin");
        when(jwtTokenUtil.extractRole(token)).thenReturn(1);
        when(userRepository.findByLoginName("admin")).thenReturn(adminUser);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtil, times(1)).extractUsername(token);
        verify(jwtTokenUtil, times(1)).extractRole(token);
        verify(userRepository, times(1)).findByLoginName("admin");
        assert SecurityContextHolder.getContext().getAuthentication() != null;
        assert SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    public void testDoFilterInternal_DisabledUser() throws ServletException, IOException {
        // Arrange
        String token = "disabled.user.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenUtil.extractUsername(token)).thenReturn("disabled");
        when(jwtTokenUtil.extractRole(token)).thenReturn(2);
        when(userRepository.findByLoginName("disabled")).thenReturn(disabledUser);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtil, times(1)).extractUsername(token);
        verify(userRepository, times(1)).findByLoginName("disabled");
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    public void testDoFilterInternal_UserNotFound() throws ServletException, IOException {
        // Arrange
        String token = "unknown.user.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenUtil.extractUsername(token)).thenReturn("unknown");
        when(jwtTokenUtil.extractRole(token)).thenReturn(2);
        when(userRepository.findByLoginName("unknown")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtil, times(1)).extractUsername(token);
        verify(userRepository, times(1)).findByLoginName("unknown");
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    public void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        // Arrange
        String token = "invalid.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenUtil.extractUsername(token)).thenThrow(new RuntimeException("Invalid token"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtil, times(1)).extractUsername(token);
        verify(userRepository, never()).findByLoginName(anyString());
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }
}