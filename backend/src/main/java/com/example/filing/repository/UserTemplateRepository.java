package com.example.filing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 根据用户ID和模板ID查询用户模板
     *
     * @param userId     用户ID
     * @param templateId 模板ID
     * @return 用户模板
     */
    UserTemplate findByUserIdAndTemplateId(String userId, String templateId);

    /**
     * 查询所有用户模板，支持过滤条件
     * 注意：此查询假设存在连接TemplateRegistry表的关系，
     * 如果实际使用中涉及连接查询，可能需要调整此SQL以匹配实际关系。
     * 
     * @param pageable     分页参数
     * @param templateName 模板名称（可选）
     * @param templateCode 模板编号（可选）
     * @param status       状态值（可选）
     * @return 用户模板分页列表
     */
    @Query("SELECT ut FROM UserTemplate ut " +
            "JOIN TemplateRegistry tr ON ut.templateId = tr.id " +
            "WHERE (:templateName IS NULL OR tr.templateName LIKE CONCAT('%',:templateName,'%')) " +
            "AND (:templateCode IS NULL OR tr.templateCode LIKE CONCAT('%',:templateCode,'%')) " +
            "AND (:status IS NULL OR ut.status = :status)")
    Page<UserTemplate> findAllWithFilters(
            Pageable pageable,
            @Param("templateName") String templateName,
            @Param("templateCode") String templateCode,
            @Param("status") Integer status);
}