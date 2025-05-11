package com.example.filing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.filing.dto.AuthenticationRequest;
import com.example.filing.dto.AuthenticationResponse;
import com.example.filing.dto.request.UserRegistrationRequest;
import com.example.filing.entity.SysUser;
import com.example.filing.exception.BusinessException;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.security.JwtAuthenticationFilter;
import com.example.filing.service.UserService;
import com.example.filing.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
public class AuthControllerTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserService userService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockBean
        private JwtTokenUtil jwtTokenUtil;

        @MockBean
        private PasswordEncoder passwordEncoder;

        @MockBean
        private SysUserRepository userRepository;

        private AuthenticationRequest validUserRequest;
        private AuthenticationRequest validAdminRequest;
        private AuthenticationRequest invalidRequest;
        private UserRegistrationRequest registrationRequest;
        private AuthenticationResponse userResponse;
        private AuthenticationResponse adminResponse;
        private SysUser registeredUser;

        @PostConstruct
        public void setupMockMvc() {
                this.mockMvc = MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .build();
        }

        @BeforeEach
        public void setup() {
                // Setup valid user request
                validUserRequest = new AuthenticationRequest();
                validUserRequest.setLoginName("user");
                validUserRequest.setPassword("password");

                // Setup valid admin request
                validAdminRequest = new AuthenticationRequest();
                validAdminRequest.setLoginName("admin");
                validAdminRequest.setPassword("admin-password");

                // Setup invalid request
                invalidRequest = new AuthenticationRequest();
                invalidRequest.setLoginName("invalid");
                invalidRequest.setPassword("invalid-password");

                // Setup registration request
                registrationRequest = new UserRegistrationRequest();
                registrationRequest.setLoginName("newuser");
                registrationRequest.setPassword("password");
                registrationRequest.setUserName("New User");
                registrationRequest.setRole(2);

                // Setup user response
                userResponse = new AuthenticationResponse();
                userResponse.setToken("user-jwt-token");
                userResponse.setUserName("Regular User");
                userResponse.setRole(2);

                // Setup admin response
                adminResponse = new AuthenticationResponse();
                adminResponse.setToken("admin-jwt-token");
                adminResponse.setUserName("Admin User");
                adminResponse.setRole(1);

                // Setup registered user
                registeredUser = new SysUser();
                registeredUser.setId("new-user-id");
                registeredUser.setLoginName("newuser");
                registeredUser.setPassword("encoded-password");
                registeredUser.setUserName("New User");
                registeredUser.setRole(2);
                registeredUser.setStatus(1);
                registeredUser.setCreateTime(LocalDateTime.now());
                registeredUser.setUpdateTime(LocalDateTime.now());
        }

        @Test
        public void testUserLogin_Success() throws Exception {
                // Arrange
                when(userService.authenticate(any(AuthenticationRequest.class))).thenReturn(userResponse);

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"loginName\":\"user\",\"password\":\"password\"}"))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isOk());
        }

        @Test
        public void testUserLogin_InvalidCredentials() throws Exception {
                // Arrange
                doThrow(new BadCredentialsException("Invalid password"))
                                .when(userService).authenticate(any(AuthenticationRequest.class));

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"loginName\":\"invalid\",\"password\":\"invalid-password\"}"))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void testUserLogin_UserNotFound() throws Exception {
                // Arrange
                doThrow(new UsernameNotFoundException("User not found"))
                                .when(userService).authenticate(any(AuthenticationRequest.class));

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"loginName\":\"invalid\",\"password\":\"invalid-password\"}"))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isNotFound());
        }

        @Test
        public void testAdminLogin_Success() throws Exception {
                // Arrange
                when(userService.adminAuthenticate(any(AuthenticationRequest.class))).thenReturn(adminResponse);

                // Act & Assert
                mockMvc.perform(post("/api/auth/admin/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"loginName\":\"admin\",\"password\":\"admin-password\"}"))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isOk());
        }

        @Test
        public void testRegister_Success() throws Exception {
                // Arrange
                when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(registeredUser);

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"loginName\":\"newuser\",\"password\":\"password\",\"userName\":\"New User\",\"role\":2}"))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isOk());
        }

        @Test
        public void testRegister_DuplicateUser() throws Exception {
                // Arrange
                doThrow(new BusinessException("Username already exists"))
                                .when(userService).registerUser(any(UserRegistrationRequest.class));

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"loginName\":\"newuser\",\"password\":\"password\",\"userName\":\"New User\",\"role\":2}"))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isInternalServerError());
        }
}