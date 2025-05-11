package com.example.filing.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    private String id;

    @Column(name = "login_name", unique = true, nullable = false, length = 50)
    private String loginName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "role", nullable = false)
    private Integer role;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}