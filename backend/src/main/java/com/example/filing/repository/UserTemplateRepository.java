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
         * 根据状态统计数量
         * 
         * @param status 状态值
         * @return 符合条件的记录数
         */
        long countByStatus(int status);

        /**
         * 统计所有记录数
         * 
         * @return 总记录数
         */
        @Override
        long count();

        /**
         * 根据参数过滤条件查询所有用户模板（管理员用）
         * 
         * @param pageable     分页参数
         * @param templateName 模板名称（可选）
         * @param templateCode 模板编号（可选）
         * @param status       状态（可选）
         * @return 符合条件的用户模板分页列表
         */
        @Query("SELECT ut FROM UserTemplate ut JOIN TemplateRegistry tr ON ut.templateId = tr.id " +
                        "WHERE (:templateName IS NULL OR tr.templateName LIKE CONCAT('%', :templateName, '%')) " +
                        "AND (:templateCode IS NULL OR tr.templateCode LIKE CONCAT('%', :templateCode, '%')) " +
                        "AND (:status IS NULL OR ut.status = :status)")
        Page<UserTemplate> findAllWithFilters(
                        Pageable pageable,
                        @Param("templateName") String templateName,
                        @Param("templateCode") String templateCode,
                        @Param("status") Integer status);

        /**
         * 根据用户ID及其他参数过滤条件查询用户模板
         * 
         * @param userId       用户ID
         * @param pageable     分页参数
         * @param templateName 模板名称（可选）
         * @param templateCode 模板编号（可选）
         * @param status       状态（可选）
         * @return 符合条件的用户模板分页列表
         */
        @Query("SELECT ut FROM UserTemplate ut JOIN TemplateRegistry tr ON ut.templateId = tr.id " +
                        "WHERE ut.userId = :userId " +
                        "AND (:templateName IS NULL OR tr.templateName LIKE CONCAT('%', :templateName, '%')) " +
                        "AND (:templateCode IS NULL OR tr.templateCode LIKE CONCAT('%', :templateCode, '%')) " +
                        "AND (:status IS NULL OR ut.status = :status)")
        Page<UserTemplate> findByUserIdWithFilters(
                        @Param("userId") String userId,
                        Pageable pageable,
                        @Param("templateName") String templateName,
                        @Param("templateCode") String templateCode,
                        @Param("status") Integer status);

        /**
         * 根据参数过滤条件查询所有用户模板（管理员用），同时返回模板信息和用户信息
         * 
         * @param pageable     分页参数
         * @param templateName 模板名称（可选）
         * @param templateCode 模板编号（可选）
         * @param status       状态（可选）
         * @return 包含UserTemplate、TemplateRegistry和SysUser字段的结果集
         */
        @Query("SELECT ut, tr, su FROM UserTemplate ut " +
                        "JOIN TemplateRegistry tr ON ut.templateId = tr.id " +
                        "JOIN SysUser su ON ut.userId = su.id " +
                        "WHERE (:templateName IS NULL OR tr.templateName LIKE CONCAT('%', :templateName, '%')) " +
                        "AND (:templateCode IS NULL OR tr.templateCode LIKE CONCAT('%', :templateCode, '%')) " +
                        "AND (:status IS NULL OR ut.status = :status)")
        Page<Object[]> findAllWithTemplateInfoFilters(
                        Pageable pageable,
                        @Param("templateName") String templateName,
                        @Param("templateCode") String templateCode,
                        @Param("status") Integer status);

        /**
         * 根据用户ID及其他参数过滤条件查询用户模板，同时返回模板信息和用户信息
         * 
         * @param userId       用户ID
         * @param pageable     分页参数
         * @param templateName 模板名称（可选）
         * @param templateCode 模板编号（可选）
         * @param status       状态（可选）
         * @return 包含UserTemplate、TemplateRegistry和SysUser字段的结果集
         */
        @Query("SELECT ut, tr, su FROM UserTemplate ut " +
                        "JOIN TemplateRegistry tr ON ut.templateId = tr.id " +
                        "JOIN SysUser su ON ut.userId = su.id " +
                        "WHERE ut.userId = :userId " +
                        "AND (:templateName IS NULL OR tr.templateName LIKE CONCAT('%', :templateName, '%')) " +
                        "AND (:templateCode IS NULL OR tr.templateCode LIKE CONCAT('%', :templateCode, '%')) " +
                        "AND (:status IS NULL OR ut.status = :status)")
        Page<Object[]> findByUserIdWithTemplateInfoFilters(
                        @Param("userId") String userId,
                        Pageable pageable,
                        @Param("templateName") String templateName,
                        @Param("templateCode") String templateCode,
                        @Param("status") Integer status);

        /**
         * 检查用户是否有访问指定模板的权限
         * 
         * @param userId     用户ID
         * @param templateId 模板ID
         * @return 是否存在用户模板关系
         */
        boolean existsByUserIdAndTemplateId(String userId, String templateId);
}