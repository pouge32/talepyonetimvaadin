package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.base.entity.MessageEntity;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Integer> {
    
    List<MessageEntity> findByRequest_RequestIdOrderBySentAtAsc(Integer requestId);

    int countByRequest_RequestIdAndReceiver_UserIdAndIsRead(Integer requestId, Integer receiverId, Integer isRead);
    
    List<MessageEntity> findByRequest_RequestIdAndReceiver_UserIdAndIsRead(Integer requestId, Integer receiverId, Integer isRead);
}