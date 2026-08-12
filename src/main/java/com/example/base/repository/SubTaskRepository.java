package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.SubTaskEntity;

public interface SubTaskRepository extends JpaRepository<SubTaskEntity, Integer> {
    
    List<SubTaskEntity> findByWorkflow_TaskIdOrderByIdAsc(Integer workflowId);
}