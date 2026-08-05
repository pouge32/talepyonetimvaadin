package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {
    List<CategoryEntity> findByActiveTrue();
    boolean existsByNameIgnoreCase(String name);
}