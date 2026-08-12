package com.example.base.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.NotificationEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.NotificationRepository;
import com.example.base.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SettingsService settingsService;
    private final NotificationBroadcaster broadcaster; 

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, SettingsService settingsService, NotificationBroadcaster broadcaster) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.broadcaster = broadcaster;
    }

    public List<NotificationEntity> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByUser_UserIdAndIsReadOrderByCreatedAtDesc(userId, 0);
    }

    @Transactional
    public void markAsRead(NotificationEntity notification) {
        notification.setIsRead(1);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyUser(Integer userId, String title, String content) {
        if (!settingsService.isNotificationsEnabled()) {
        return; 
    }
        
        if (userId == null) return;
        
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Bildirim atılacak kullanıcı bulunamadı (ID: " + userId + ")"));
                
        NotificationEntity bildirim = new NotificationEntity();
        bildirim.setUser(user);
        bildirim.setTitle(title);
        bildirim.setContent(content);
        bildirim.setIsRead(0);
        bildirim.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(bildirim);
        
        broadcaster.broadcast(userId);
    }
    
    @Transactional
    public void notifyRole(String roleName, String title, String content) {
        userRepository.findAll().stream()
            .filter(u -> u.getRole() != null && u.getRole().name().equals(roleName))
            .forEach(user -> notifyUser(user.getUserId(), title, content));
    }

    @Transactional
    public void deleteNotification(Integer notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}