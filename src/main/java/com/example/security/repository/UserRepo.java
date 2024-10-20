package com.example.security.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.security.entity.User;

public interface UserRepo extends JpaRepository<User, String> {

    @Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?1")
    User findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = ?1")
    User findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.userId = :userId")
    void updateUserEnabled(@Param("userId") String userId, @Param("enabled") boolean enabled);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO user_roles (user_user_id, roles) VALUES (:userId, :role)", nativeQuery = true)
    void setRoleToUserByUserId(@Param("userId") String userId, @Param("role") String role);

}
