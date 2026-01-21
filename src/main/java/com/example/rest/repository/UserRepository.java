package com.example.rest.repository;

import com.example.rest.entity.User;
import com.example.rest.entity.enums.UserStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Find by single property
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // Find by multiple properties (AND)
    Optional<User> findByUsernameAndEmail(String username, String email);

    // Find by property with condition
    List<User> findByStatus(UserStatusEnum status);
    List<User> findByFirstNameContainingIgnoreCase(String firstName);

    // Existence checks
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Count queries
    long countByStatus(UserStatusEnum status);

    // Delete queries
    long deleteByStatus(UserStatusEnum status);

    // Find with sorting
    List<User> findByStatusOrderByCreatedAtDesc(UserStatusEnum status);

    // Find with pagination
    Page<User> findByStatus(UserStatusEnum status, Pageable pageable);

    // Simple JPQL query
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailJPQL(@Param("email") String email);

    // Join query
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    // Complex condition
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    // Aggregation query
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatusJPQL(@Param("status") UserStatusEnum status);

    // Modifying query
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateUserStatus(@Param("id") Long id, @Param("status") UserStatusEnum status);

    // Delete query
    @Modifying
    @Query("DELETE FROM User u WHERE u.status = :status AND u.createdAt < :date")
    int deleteInactiveUsersBefore(@Param("status") UserStatusEnum status,
                                  @Param("date") LocalDateTime date);

}
