package com.example.filing.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.filing.entity.SysUser;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Spring Security用户详情实现
 */
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private final String loginName;
    @JsonIgnore
    private final String password;
    private final Integer role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String id, String username, String loginName, String password, Integer role,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.loginName = loginName;
        this.password = password;
        this.role = role;
        this.authorities = authorities;
    }

    /**
     * 从SysUser创建UserDetailsImpl
     * 
     * @param user 系统用户
     * @return UserDetailsImpl实例
     */
    public static UserDetailsImpl build(SysUser user) {
        String roleAuthority = "ROLE_" + (user.getRole() == 1 ? "ADMIN" : "USER");
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleAuthority));

        return new UserDetailsImpl(
                user.getId(),
                user.getUserName(),
                user.getLoginName(),
                user.getPassword(),
                user.getRole(),
                authorities);
    }

    /**
     * 获取用户ID
     * 
     * @return 用户ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取登录名
     * 
     * @return 登录名
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * 获取用户角色
     * 
     * @return 用户角色
     */
    public Integer getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}