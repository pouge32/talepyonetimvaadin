package com.example.base.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.base.entity.SystemLogEntity;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Long> {
    
    @Query("SELECT s FROM SystemLogEntity s WHERE s.action LIKE %:keyword% ORDER BY s.createdAt DESC")
    List<SystemLogEntity> findByActionContaining(@Param("keyword") String keyword);
    @Query("SELECT l FROM SystemLogEntity l WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR LOWER(l.action) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:userEmail IS NULL OR :userEmail = '' OR LOWER(l.action) LIKE LOWER(CONCAT('%', :userEmail, '%'))) AND " +
           "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.createdAt <= :endDate)")
    Page<SystemLogEntity> findFilteredLogs(@Param("searchTerm") String searchTerm, 
                                           @Param("userEmail") String userEmail, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           Pageable pageable);
}