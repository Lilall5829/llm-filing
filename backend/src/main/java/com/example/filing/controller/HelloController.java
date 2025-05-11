package com.example.filing.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.filing.repository.SysUserRepository;

/**
 * 简单的HelloWorld控制器
 */
@RestController
public class HelloController {

    @Autowired
    private SysUserRepository userRepository;

    /**
     * HelloWorld端点
     * 
     * @return 问候消息
     */
    @GetMapping("/hello")
    public String hello() {
        long userCount = userRepository.count();
        return "Hello, Filing System! There are " + userCount + " users in the system.";
    }
}