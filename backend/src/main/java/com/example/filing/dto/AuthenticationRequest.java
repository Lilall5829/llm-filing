package com.example.filing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录请求DTO")
public class AuthenticationRequest {
    @Schema(description = "登录用户名", example = "user123", required = true)
    private String loginName;

    @Schema(description = "登录密码", example = "password123", required = true)
    private String password;
}