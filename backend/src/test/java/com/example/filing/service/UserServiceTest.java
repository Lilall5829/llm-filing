package com.example.filing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.filing.dto.request.ResetPasswordRequest;
import com.example.filing.dto.request.UserRegistrationRequest;
import com.example.filing.dto.request.UserUpdateRequest;
import com.example.filing.entity.SysUser;
import com.example.filing.exception.BusinessException;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.service.impl.UserServiceImpl;

public class UserServiceTest {

    @Mock
    private SysUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private SysUser adminUser;
    private SysUser regularUser;
    private List<SysUser> userList;
    private UserRegistrationRequest registrationRequest;
    private UserUpdateRequest updateRequest;
    private ResetPasswordRequest resetPasswordRequest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup admin user
        adminUser = new SysUser();
        adminUser.setId("admin-id-1");
        adminUser.setLoginName("admin");
        adminUser.setPassword("encoded-admin-password");
        adminUser.setUserName("Admin User");
        adminUser.setRole(1);
        adminUser.setStatus(1);
        adminUser.setCreateTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());

        // Setup regular user
        regularUser = new SysUser();
        regularUser.setId("user-id-1");
        regularUser.setLoginName("user");
        regularUser.setPassword("encoded-password");
        regularUser.setUserName("Regular User");
        regularUser.setRole(2);
        regularUser.setStatus(1);
        regularUser.setCreateTime(LocalDateTime.now());
        regularUser.setUpdateTime(LocalDateTime.now());

        // Setup user list
        userList = new ArrayList<>();
        userList.add(adminUser);
        userList.add(regularUser);

        // Setup registration request
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setLoginName("newuser");
        registrationRequest.setPassword("password123");
        registrationRequest.setUserName("New User");
        registrationRequest.setRole(2);

        // Setup update request
        updateRequest = new UserUpdateRequest();
        updateRequest.setUserName("Updated User");
        updateRequest.setRole(2);
        updateRequest.setStatus(1);

        // Setup reset password request
        resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setPassword("newpassword123");
    }

    @Test
    public void testFindUserByLoginName_Success() {
        // Arrange
        when(userRepository.findByLoginName("user")).thenReturn(regularUser);

        // Act
        SysUser foundUser = userService.findUserByLoginName("user");

        // Assert
        assertNotNull(foundUser);
        assertEquals("user", foundUser.getLoginName());
        assertEquals("Regular User", foundUser.getUserName());
        assertEquals(2, foundUser.getRole());
        verify(userRepository, times(1)).findByLoginName("user");
    }

    @Test
    public void testFindUserByLoginName_UserNotFound() {
        // Arrange
        when(userRepository.findByLoginName("nonexistent")).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            userService.findUserByLoginName("nonexistent");
        });
        verify(userRepository, times(1)).findByLoginName("nonexistent");
    }

    @Test
    public void testFindAllUsers_Success() {
        // Arrange
        Page<SysUser> userPage = new PageImpl<>(userList, PageRequest.of(0, 10), userList.size());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);

        // Act
        Page<SysUser> result = userService.findAllUsers(null, null, null, 1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("admin", result.getContent().get(0).getLoginName());
        assertEquals("user", result.getContent().get(1).getLoginName());
        verify(userRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    public void testRegisterUser_Success() {
        // Arrange
        when(userRepository.findByLoginName("newuser")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password-123");
        when(userRepository.save(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser savedUser = invocation.getArgument(0);
            savedUser.setId(UUID.randomUUID().toString());
            return savedUser;
        });

        // Act
        SysUser newUser = userService.registerUser(registrationRequest);

        // Assert
        assertNotNull(newUser);
        assertNotNull(newUser.getId());
        assertEquals("newuser", newUser.getLoginName());
        assertEquals("New User", newUser.getUserName());
        assertEquals("encoded-password-123", newUser.getPassword());
        assertEquals(2, newUser.getRole());
        assertEquals(1, newUser.getStatus());
        verify(userRepository, times(1)).findByLoginName("newuser");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(SysUser.class));
    }

    @Test
    public void testRegisterUser_DuplicateUser() {
        // Arrange
        when(userRepository.findByLoginName("user")).thenReturn(regularUser);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            registrationRequest.setLoginName("user");
            userService.registerUser(registrationRequest);
        });
        verify(userRepository, times(1)).findByLoginName("user");
        verify(userRepository, never()).save(any(SysUser.class));
    }

    @Test
    public void testUpdateUser_Success() {
        // Arrange
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SysUser updatedUser = userService.updateUser("user-id-1", updateRequest);

        // Assert
        assertNotNull(updatedUser);
        assertEquals("user-id-1", updatedUser.getId());
        assertEquals("user", updatedUser.getLoginName());
        assertEquals("Updated User", updatedUser.getUserName());
        assertEquals(2, updatedUser.getRole());
        assertEquals(1, updatedUser.getStatus());
        verify(userRepository, times(1)).findById("user-id-1");
        verify(userRepository, times(1)).save(any(SysUser.class));
    }

    @Test
    public void testUpdateUser_UserNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            userService.updateUser("nonexistent-id", updateRequest);
        });
        verify(userRepository, times(1)).findById("nonexistent-id");
        verify(userRepository, never()).save(any(SysUser.class));
    }

    @Test
    public void testDeleteUser_Success() {
        // Arrange
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(regularUser));

        // Act
        userService.deleteUser("user-id-1");

        // Assert
        verify(userRepository, times(1)).findById("user-id-1");
        verify(userRepository, times(1)).deleteById("user-id-1");
    }

    @Test
    public void testDeleteUser_UserNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            userService.deleteUser("nonexistent-id");
        });
        verify(userRepository, times(1)).findById("nonexistent-id");
        verify(userRepository, never()).deleteById(anyString());
    }

    @Test
    public void testResetPassword_Success() {
        // Arrange
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded-new-password");
        when(userRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.resetPassword("user-id-1", resetPasswordRequest);

        // Assert
        assertEquals("encoded-new-password", regularUser.getPassword());
        verify(userRepository, times(1)).findById("user-id-1");
        verify(passwordEncoder, times(1)).encode("newpassword123");
        verify(userRepository, times(1)).save(eq(regularUser));
    }

    @Test
    public void testResetPassword_UserNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            userService.resetPassword("nonexistent-id", resetPasswordRequest);
        });
        verify(userRepository, times(1)).findById("nonexistent-id");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(SysUser.class));
    }
}