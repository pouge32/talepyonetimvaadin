package com.example.base.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.RequestEntity;

public interface RequestRepository extends JpaRepository<RequestEntity, Integer> {

    List<RequestEntity> findByCustomer_UserId(Integer customerId);

    List<RequestEntity> findByCustomer_UserIdAndCreatedAtAfter(Integer customerId, LocalDateTime since);

    List<RequestEntity> findByStatus(String status);

    List<RequestEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);
}