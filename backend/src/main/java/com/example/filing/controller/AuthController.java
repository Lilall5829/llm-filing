package com.example.filing.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.filing.dto.AuthenticationRequest;
import com.example.filing.dto.AuthenticationResponse;
import com.example.filing.dto.request.UserRegistrationRequest;
import com.example.filing.entity.SysUser;
import com.example.filing.exception.BusinessException;
import com.example.filing.service.UserService;
import com.example.filing.util.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户登录、注册相关接口")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "普通用户登录接口")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ResponseEntity<Result<AuthenticationResponse>> login(
            @Parameter(description = "登录请求信息", required = true) @RequestBody AuthenticationRequest request)
            throws UsernameNotFoundException, BadCredentialsException {
        AuthenticationResponse response = userService.authenticate(request);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "管理员登录", description = "管理员用户登录接口")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ResponseEntity<Result<AuthenticationResponse>> adminLogin(
            @Parameter(description = "登录请求信息", required = true) @RequestBody AuthenticationRequest request)
            throws UsernameNotFoundException, BadCredentialsException {
        AuthenticationResponse response = userService.adminAuthenticate(request);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "普通用户注册接口")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "注册信息不合法")
    })
    public ResponseEntity<Result<SysUser>> register(
            @Parameter(description = "注册请求信息", required = true) @RequestBody UserRegistrationRequest request)
            throws BusinessException {
        SysUser user = userService.registerUser(request);
        return ResponseEntity.ok(Result.success(user));
    }
}