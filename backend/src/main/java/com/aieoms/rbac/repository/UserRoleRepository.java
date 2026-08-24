package com.aieoms.rbac.repository;

import com.aieoms.rbac.entity.UserRole;
import com.aieoms.rbac.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(Long userId);

    boolean existsByUserIdAndRoleId(
            Long userId,
            Long roleId
    );

    @Query(
        value = """
            SELECT r.name
            FROM user_roles ur
            INNER JOIN roles r
                ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            ORDER BY r.id
            """,
        nativeQuery = true
    )
    List<String> findRoleNamesByUserId(
            @Param("userId") Long userId
    );
}