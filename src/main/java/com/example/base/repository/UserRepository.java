package com.example.base.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserEntity> findByRole(Role role);

    List<UserEntity> findByRegistrationStatus(String registrationStatus);

    List<UserEntity> findByStatus(String status);

    List<UserEntity> findByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}