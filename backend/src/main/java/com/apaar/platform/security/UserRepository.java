package com.apaar.platform.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
            SELECT DISTINCT rp.permission_code
            FROM permissions p
            JOIN role_permissions rp ON rp.permission_code = p.code
            JOIN user_roles ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<String> findPermissionCodes(@Param("userId") UUID userId);
}
