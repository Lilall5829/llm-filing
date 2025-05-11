package com.example.filing.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    private String loginName;
    private String password;
    private String userName;
    private Integer role;

    public UserRegistrationRequest(String loginName, String password, String userName) {
        this.loginName = loginName;
        this.password = password;
        this.userName = userName;
        this.role = 2; // 默认为普通用户
    }
}