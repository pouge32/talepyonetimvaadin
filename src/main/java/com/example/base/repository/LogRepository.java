package com.example.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.base.entity.LogEntity;

@Repository
public interface LogRepository extends JpaRepository<LogEntity, Long> {
}