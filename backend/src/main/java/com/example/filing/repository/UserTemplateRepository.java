package com.example.filing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.filing.entity.UserTemplate;

@Repository
public interface UserTemplateRepository extends JpaRepository<UserTemplate, String> {

    /**
     * 根据用户ID分页查询用户模板列表
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 用户模板分页列表
     */
    Page<UserTemplate> findByUserId(String userId, Pageable pageable);
}