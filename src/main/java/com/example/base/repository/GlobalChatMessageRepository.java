package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.base.entity.GlobalChatMessageEntity;

public interface GlobalChatMessageRepository extends JpaRepository<GlobalChatMessageEntity, Integer> {
    
    @Query("SELECT m FROM GlobalChatMessageEntity m JOIN FETCH m.sender LEFT JOIN FETCH m.readByUsers ORDER BY m.createdAt ASC")
    List<GlobalChatMessageEntity> findAllWithSenderAndReads();
}