package com.example.filing.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.filing.entity.TemplateRegistry;

/**
 * 模板数据访问层
 */
@Repository
public interface TemplateRegistryRepository extends JpaRepository<TemplateRegistry, String> {

        /**
         * 分页查询模板列表
         * 
         * @param templateCode 模板编号
         * @param templateName 模板名称
         * @param templateType 模板类型
         * @param pageable     分页参数
         * @return 模板分页数据
         */
        @Query("SELECT t FROM TemplateRegistry t WHERE "
                        + "(:templateCode IS NULL OR t.templateCode LIKE CONCAT('%',:templateCode,'%')) AND "
                        + "(:templateName IS NULL OR t.templateName LIKE CONCAT('%',:templateName,'%')) AND "
                        + "(:templateType IS NULL OR t.templateType = :templateType)")
        Page<TemplateRegistry> findTemplates(
                        @Param("templateCode") String templateCode,
                        @Param("templateName") String templateName,
                        @Param("templateType") String templateType,
                        Pageable pageable);

        /**
         * 统计未删除的模板数量
         * 
         * @return 未删除的模板数量
         */
        long countByDeletedFalse();

        /**
         * 根据模板编号查找模板
         * 
         * @param templateCode 模板编号
         * @return 模板实体（可选）
         */
        Optional<TemplateRegistry> findByTemplateCode(String templateCode);
}