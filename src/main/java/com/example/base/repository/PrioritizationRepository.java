package com.example.base.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.PrioritizationEntity;

public interface PrioritizationRepository extends JpaRepository<PrioritizationEntity, Integer> {

    Optional<PrioritizationEntity> findByRequest_RequestId(Integer requestId);

    List<PrioritizationEntity> findAllByOrderByPriorityScoreDesc();

    List<PrioritizationEntity> findTop5ByOrderByPriorityScoreDesc();

    long countByUrgency(int urgency);
}