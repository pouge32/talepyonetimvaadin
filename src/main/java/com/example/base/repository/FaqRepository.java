package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.FaqEntity;

public interface FaqRepository extends JpaRepository<FaqEntity, Integer> {
    List<FaqEntity> findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(String q, String a);
}