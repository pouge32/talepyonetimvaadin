package com.example.base.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.base.entity.WorkflowEntity;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Integer> {

    Optional<WorkflowEntity> findByRequest_RequestId(Integer requestId);

    List<WorkflowEntity> findByDeveloper_UserId(Integer developerId);

    List<WorkflowEntity> findByWorkflowStatus(String workflowStatus);

    long countByWorkflowStatus(String workflowStatus);

    @Query("SELECT w FROM WorkflowEntity w JOIN FETCH w.request")
    List<WorkflowEntity> findAllWithRequests();

}