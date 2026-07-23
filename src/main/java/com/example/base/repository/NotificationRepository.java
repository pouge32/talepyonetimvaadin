package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.base.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {

    List<NotificationEntity> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    // Pop-up'ta gösterilecek okunmamış bildirimler için (is_read = 0).
    List<NotificationEntity> findByUser_UserIdAndIsRead(Integer userId, Integer isRead);
}