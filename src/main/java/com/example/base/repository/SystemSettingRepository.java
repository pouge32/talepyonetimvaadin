package com.example.base.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.SystemSettingEntity;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, String> {
    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
}