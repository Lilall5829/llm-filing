package com.example.filing.service;

import org.springframework.data.domain.Page;

import com.example.filing.dto.AuthenticationRequest;
import com.example.filing.dto.AuthenticationResponse;
import com.example.filing.dto.request.ResetPasswordRequest;
import com.example.filing.dto.request.UserRegistrationRequest;
import com.example.filing.dto.request.UserUpdateRequest;
import com.example.filing.entity.SysUser;

public interface UserService {

    /**
     * 普通用户登录认证
     * 
     * @param request 登录请求
     * @return 认证响应
     */
    AuthenticationResponse authenticate(AuthenticationRequest request);

    /**
     * 管理员登录认证
     * 
     * @param request 登录请求
     * @return 认证响应
     */
    AuthenticationResponse adminAuthenticate(AuthenticationRequest request);

    /**
     * 注册用户
     * 
     * @param request 注册请求
     * @return 用户实体
     */
    SysUser registerUser(UserRegistrationRequest request);

    /**
     * 根据登录名查找用户
     * 
     * @param loginName 登录名
     * @return 用户实体
     */
    SysUser findUserByLoginName(String loginName);

    /**
     * 更新最后登录时间
     * 
     * @param loginName 登录名
     */
    void updateLastLoginTime(String loginName);

    /**
     * 分页查询用户列表
     * 
     * @param loginName 登录名(可选)
     * @param userName  用户名(可选)
     * @param role      角色(可选)
     * @param current   当前页
     * @param pageSize  每页大小
     * @return 用户分页列表
     */
    Page<SysUser> findAllUsers(String loginName, String userName, Integer role, Integer current, Integer pageSize);

    /**
     * 更新用户信息
     * 
     * @param id      用户ID
     * @param request 更新请求
     * @return 更新后的用户
     */
    SysUser updateUser(String id, UserUpdateRequest request);

    /**
     * 删除用户
     * 
     * @param id 用户ID
     */
    void deleteUser(String id);

    /**
     * 重置用户密码
     * 
     * @param id      用户ID
     * @param request 重置密码请求
     */
    void resetPassword(String id, ResetPasswordRequest request);
}