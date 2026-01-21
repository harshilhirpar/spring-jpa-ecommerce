package com.example.rest.repository;

import com.example.rest.entity.Role;
import com.example.rest.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Find role by name
    Optional<Role> findByName(RoleType name);
    // Use case: Get ROLE_ADMIN when assigning admin privileges

    // Check if role exists
    boolean existsByName(RoleType name);
    // Use case: Ensure default roles exist during app startup
}
