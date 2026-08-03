package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.base.entity.SystemLogEntity;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Long> {
    
    // "message" olan yerleri "action" olarak güncelledik
    @Query("SELECT s FROM SystemLogEntity s WHERE s.action LIKE %:keyword% ORDER BY s.createdAt DESC")
    List<SystemLogEntity> findByActionContaining(@Param("keyword") String keyword);
}