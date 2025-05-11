package com.example.filing.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.filing.dto.request.ResetPasswordRequest;
import com.example.filing.dto.request.UserUpdateRequest;
import com.example.filing.entity.SysUser;
import com.example.filing.service.UserService;
import com.example.filing.util.Result;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表
     * 
     * @param loginName 登录名（可选）
     * @param userName  用户名（可选）
     * @param role      角色（可选）
     * @param current   当前页
     * @param pageSize  每页大小
     * @return 用户分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Page<SysUser>>> getUserList(
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<SysUser> userPage = userService.findAllUsers(loginName, userName, role, current, pageSize);
        return ResponseEntity.ok(Result.success(userPage));
    }

    /**
     * 更新用户信息
     * 
     * @param id      用户ID
     * @param request 更新请求
     * @return 更新后的用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<SysUser>> updateUser(@PathVariable String id, @RequestBody UserUpdateRequest request) {
        SysUser updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(Result.success(updatedUser));
    }

    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<String>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Result.success("用户删除成功"));
    }

    /**
     * 重置用户密码
     * 
     * @param id      用户ID
     * @param request 重置密码请求
     * @return 操作结果
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<String>> resetPassword(
            @PathVariable String id,
            @RequestBody ResetPasswordRequest request) {

        userService.resetPassword(id, request);
        return ResponseEntity.ok(Result.success("密码重置成功"));
    }
}