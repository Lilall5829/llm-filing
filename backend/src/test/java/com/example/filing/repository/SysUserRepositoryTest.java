package com.example.filing.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.example.filing.entity.SysUser;

import jakarta.persistence.criteria.Predicate;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Sql(scripts = "/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SysUserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Test
    public void testFindByLoginName() {
        // 准备测试数据
        SysUser user = new SysUser();
        user.setLoginName("testuser");
        user.setPassword("password");
        user.setUserName("Test User");
        user.setRole(2);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        entityManager.persist(user);
        entityManager.flush();

        // 执行测试
        SysUser found = sysUserRepository.findByLoginName("testuser");

        // 验证结果
        assertEquals("testuser", found.getLoginName());
        assertEquals("Test User", found.getUserName());
    }

    @Test
    public void testFindAllWithSpecification() {
        // 准备测试数据
        SysUser adminUser = new SysUser();
        adminUser.setLoginName("admin");
        adminUser.setPassword("password");
        adminUser.setUserName("Admin User");
        adminUser.setRole(1);
        adminUser.setStatus(1);
        adminUser.setCreateTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());
        entityManager.persist(adminUser);

        SysUser regularUser = new SysUser();
        regularUser.setLoginName("user");
        regularUser.setPassword("password");
        regularUser.setUserName("Regular User");
        regularUser.setRole(2);
        regularUser.setStatus(1);
        regularUser.setCreateTime(LocalDateTime.now());
        regularUser.setUpdateTime(LocalDateTime.now());
        entityManager.persist(regularUser);

        SysUser disabledUser = new SysUser();
        disabledUser.setLoginName("disabled");
        disabledUser.setPassword("password");
        disabledUser.setUserName("Disabled User");
        disabledUser.setRole(2);
        disabledUser.setStatus(0);
        disabledUser.setCreateTime(LocalDateTime.now());
        disabledUser.setUpdateTime(LocalDateTime.now());
        entityManager.persist(disabledUser);

        entityManager.flush();

        // 测试通过角色筛选
        Specification<SysUser> roleSpec = (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("role"), 1);
        };
        List<SysUser> adminUsers = sysUserRepository.findAll(roleSpec);
        assertEquals(1, adminUsers.size());
        assertEquals("admin", adminUsers.get(0).getLoginName());

        // 测试通过状态筛选
        Specification<SysUser> statusSpec = (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("status"), 0);
        };
        List<SysUser> disabledUsers = sysUserRepository.findAll(statusSpec);
        assertEquals(1, disabledUsers.size());
        assertEquals("disabled", disabledUsers.get(0).getLoginName());

        // 测试复合条件筛选
        Specification<SysUser> complexSpec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("role"), 2));
            predicates.add(criteriaBuilder.equal(root.get("status"), 1));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        List<SysUser> activeRegularUsers = sysUserRepository.findAll(complexSpec);
        assertEquals(1, activeRegularUsers.size());
        assertEquals("user", activeRegularUsers.get(0).getLoginName());

        // 测试分页
        Page<SysUser> userPage = sysUserRepository.findAll(PageRequest.of(0, 2));
        assertEquals(2, userPage.getContent().size());
        assertEquals(3, userPage.getTotalElements());

        // 测试分页+条件
        Page<SysUser> filteredPage = sysUserRepository.findAll(complexSpec, PageRequest.of(0, 10));
        assertEquals(1, filteredPage.getContent().size());
        assertEquals("user", filteredPage.getContent().get(0).getLoginName());
    }
}