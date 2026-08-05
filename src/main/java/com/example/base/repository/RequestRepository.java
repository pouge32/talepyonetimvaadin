package com.example.base.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;

public interface RequestRepository extends JpaRepository<RequestEntity, Integer> {

    List<RequestEntity> findByCustomer_UserId(Integer customerId);

    List<RequestEntity> findByCustomer_UserIdAndCreatedAtAfter(Integer customerId, LocalDateTime since);

    List<RequestEntity> findByStatus(String status);

    List<RequestEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);

    long countByStatus(String status);

    long countByStatusAndCreatedAtBetween(String status, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r.category, COUNT(r) FROM RequestEntity r GROUP BY r.category")
    List<Object[]> countRequestsByCategory();

    @Query("SELECT COUNT(r) FROM RequestEntity r WHERE r.status != 'KAPATILDI'")
    long countActiveRequests();

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r.category, COUNT(r) FROM RequestEntity r WHERE r.status = 'INCELEMEDE' GROUP BY r.category")
    List<Object[]> countPendingRequestsByCategory();

    List<RequestEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
    String titleKeyword, String descriptionKeyword);

    long countByAssignedUserAndStatusIn(UserEntity assignedUser, List<String> statuses);
}