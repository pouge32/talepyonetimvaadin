package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {

    List<NotificationEntity> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    List<NotificationEntity> findByUser_UserIdAndIsRead(Integer userId, Integer isRead);

    List<NotificationEntity> findByUser_EmailAndIsReadOrderByCreatedAtDesc(String email, Integer isRead);
    
    List<NotificationEntity> findByUser_UserIdAndIsReadOrderByCreatedAtDesc(Integer userId, Integer isRead);
}