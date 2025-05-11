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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Result<AuthenticationResponse>> login(@RequestBody AuthenticationRequest request)
            throws UsernameNotFoundException, BadCredentialsException {
        AuthenticationResponse response = userService.authenticate(request);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<Result<AuthenticationResponse>> adminLogin(@RequestBody AuthenticationRequest request)
            throws UsernameNotFoundException, BadCredentialsException {
        AuthenticationResponse response = userService.adminAuthenticate(request);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/register")
    public ResponseEntity<Result<SysUser>> register(@RequestBody UserRegistrationRequest request)
            throws BusinessException {
        SysUser user = userService.registerUser(request);
        return ResponseEntity.ok(Result.success(user));
    }
}