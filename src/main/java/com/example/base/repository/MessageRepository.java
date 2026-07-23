package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, Integer> {

    List<MessageEntity> findByRequest_RequestIdOrderBySentAtAsc(Integer requestId);
}