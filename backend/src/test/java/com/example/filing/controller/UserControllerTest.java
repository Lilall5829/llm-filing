package com.example.filing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.filing.dto.request.ResetPasswordRequest;
import com.example.filing.dto.request.UserUpdateRequest;
import com.example.filing.entity.SysUser;
import com.example.filing.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserService userService;

        private SysUser adminUser;
        private SysUser regularUser;
        private ResetPasswordRequest resetPasswordRequest;
        private UserUpdateRequest userUpdateRequest;

        @BeforeEach
        public void setup() {
                adminUser = new SysUser();
                adminUser.setId("admin-id");
                adminUser.setUserName("admin");
                adminUser.setLoginName("admin");
                adminUser.setRole(1);
                adminUser.setStatus(1);

                regularUser = new SysUser();
                regularUser.setId("user-id");
                regularUser.setUserName("user");
                regularUser.setLoginName("user");
                regularUser.setRole(2);
                regularUser.setStatus(1);

                resetPasswordRequest = new ResetPasswordRequest();
                resetPasswordRequest.setPassword("newPassword");

                userUpdateRequest = new UserUpdateRequest();
                userUpdateRequest.setUserName("updatedUser");
                userUpdateRequest.setRole(2);
                userUpdateRequest.setStatus(1);

                // 创建用户列表
                Page<SysUser> userPage = new PageImpl<>(
                                Arrays.asList(adminUser, regularUser),
                                PageRequest.of(0, 10),
                                2);

                // 设置 mock 响应
                when(userService.findAllUsers(anyString(), anyString(), any(), anyInt(), anyInt()))
                                .thenReturn(userPage);

                when(userService.updateUser(anyString(), any(UserUpdateRequest.class)))
                                .thenReturn(regularUser);

                doNothing().when(userService).deleteUser(anyString());

                doNothing().when(userService).resetPassword(anyString(), any(ResetPasswordRequest.class));
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        public void testGetUserPage_Admin_Success() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.get("/api/users/page")
                                .param("current", "1")
                                .param("pageSize", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        public void testGetUserPage_User_Forbidden() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.get("/api/users/page")
                                .param("current", "1")
                                .param("pageSize", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        public void testUpdateUser_Admin_Success() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.put("/api/users/{id}", "user-id")
                                .content(objectMapper.writeValueAsString(userUpdateRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        public void testUpdateUser_User_Forbidden() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.put("/api/users/{id}", "user-id")
                                .content(objectMapper.writeValueAsString(userUpdateRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        public void testDeleteUser_Admin_Success() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/users/{id}", "user-id")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").value("用户删除成功"));
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        public void testDeleteUser_User_Forbidden() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/users/{id}", "user-id")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin", authorities = { "ROLE_ADMIN" })
        public void testResetPassword_Admin_Success() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.post("/api/users/{id}/reset-password", "user-id")
                                .content(objectMapper.writeValueAsString(resetPasswordRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").value("密码重置成功"));
        }

        @Test
        @WithMockUser(username = "user", authorities = { "ROLE_USER" })
        public void testResetPassword_User_Forbidden() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.post("/api/users/{id}/reset-password", "user-id")
                                .content(objectMapper.writeValueAsString(resetPasswordRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        public void testSecurityIsActive_Unauthorized() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.get("/api/users/page")
                                .param("current", "1")
                                .param("pageSize", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }
}