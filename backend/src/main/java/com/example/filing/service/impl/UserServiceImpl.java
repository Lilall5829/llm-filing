package com.example.filing.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.filing.dto.AuthenticationRequest;
import com.example.filing.dto.AuthenticationResponse;
import com.example.filing.dto.request.ResetPasswordRequest;
import com.example.filing.dto.request.UserRegistrationRequest;
import com.example.filing.dto.request.UserUpdateRequest;
import com.example.filing.entity.SysUser;
import com.example.filing.exception.BusinessException;
import com.example.filing.repository.SysUserRepository;
import com.example.filing.service.UserService;
import com.example.filing.util.JwtTokenUtil;

import jakarta.persistence.criteria.Predicate;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        SysUser user = userRepository.findByLoginName(request.getLoginName());
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (user.getRole() != 2) {
            throw new BadCredentialsException("Invalid user role");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("User is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // Update last login time
        updateLastLoginTime(user.getLoginName());

        // Generate JWT token
        String token = jwtTokenUtil.generateToken(user.getLoginName(), user.getRole());

        // Create response using record constructor
        return new AuthenticationResponse(token, user.getUserName(), user.getRole());
    }

    @Override
    public AuthenticationResponse adminAuthenticate(AuthenticationRequest request) {
        SysUser user = userRepository.findByLoginName(request.getLoginName());
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (user.getRole() != 1) {
            throw new BadCredentialsException("Not an admin user");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("User is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // Update last login time
        updateLastLoginTime(user.getLoginName());

        // Generate JWT token
        String token = jwtTokenUtil.generateToken(user.getLoginName(), user.getRole());

        // Create response using record constructor
        return new AuthenticationResponse(token, user.getUserName(), user.getRole());
    }

    @Override
    public SysUser registerUser(UserRegistrationRequest request) {
        // 检查用户名是否已存在
        if (userRepository.findByLoginName(request.getLoginName()) != null) {
            throw new BusinessException("Username already exists");
        }

        SysUser user = new SysUser();
        user.setId(UUID.randomUUID().toString());
        user.setLoginName(request.getLoginName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserName(request.getUserName());
        user.setRole(request.getRole() != null ? request.getRole() : 2); // 默认为普通用户
        user.setStatus(1); // 默认启用状态
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public SysUser findUserByLoginName(String loginName) {
        SysUser user = userRepository.findByLoginName(loginName);
        if (user == null) {
            throw new BusinessException("User not found with username: " + loginName);
        }
        return user;
    }

    @Override
    public void updateLastLoginTime(String loginName) {
        SysUser user = userRepository.findByLoginName(loginName);
        if (user != null) {
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Override
    public Page<SysUser> findAllUsers(String loginName, String userName, Integer role, Integer current,
            Integer pageSize) {
        // 创建动态查询条件
        Specification<SysUser> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(loginName)) {
                predicates.add(criteriaBuilder.like(root.get("loginName"), "%" + loginName + "%"));
            }

            if (StringUtils.hasText(userName)) {
                predicates.add(criteriaBuilder.like(root.get("userName"), "%" + userName + "%"));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 创建分页条件（页码从0开始）
        PageRequest pageRequest = PageRequest.of(current - 1, pageSize);

        return userRepository.findAll(specification, pageRequest);
    }

    @Override
    public SysUser updateUser(String id, UserUpdateRequest request) {
        Optional<SysUser> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new BusinessException("User not found with id: " + id);
        }

        SysUser user = optionalUser.get();

        if (StringUtils.hasText(request.getUserName())) {
            user.setUserName(request.getUserName());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        user.setUpdateTime(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(String id) {
        Optional<SysUser> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new BusinessException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
    }

    @Override
    public void resetPassword(String id, ResetPasswordRequest request) {
        Optional<SysUser> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new BusinessException("User not found with id: " + id);
        }

        SysUser user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUpdateTime(LocalDateTime.now());

        userRepository.save(user);
    }
}