package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.SystemLogEntity;

public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Integer> {

    List<SystemLogEntity> findAllByOrderByCreatedAtDesc();

    List<SystemLogEntity> findByUser_UserId(Integer userId);
}