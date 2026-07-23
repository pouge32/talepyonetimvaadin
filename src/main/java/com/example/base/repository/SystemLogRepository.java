package com.example.base.repository;

import com.example.base.entity.SystemLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Integer> {

    // Admin panelinde logları en güncelden eskiye listelemek için.
    List<SystemLogEntity> findAllByOrderByCreatedAtDesc();

    List<SystemLogEntity> findByUser_UserId(Integer userId);
}