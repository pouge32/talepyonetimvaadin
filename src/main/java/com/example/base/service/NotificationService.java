package com.example.base.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.NotificationEntity;
import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;
import com.example.base.repository.NotificationRepository;
import com.example.base.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Belirli bir role sahip tüm kullanıcılara bildirim gönderir.
     * (ör. yeni talep geldiğinde tüm PO'lara)
     */
    @Transactional
    public void notifyRole(Role role, String title, String content) {
        List<UserEntity> targets = userRepository.findByRole(role);
        for (UserEntity user : targets) {
            saveNotification(user, title, content);
        }
    }

    /**
     * Tek bir kullanıcıya, ID üzerinden bildirim gönderir.
     * ID her zaman AYNI transaction içinde yeniden çekilir; böylece
     * başka bir session'dan gelen "kopuk" (detached) referanslardan
     * kaynaklanan FK tutarsızlıkları önlenir.
     */
    @Transactional
    public void notifyUser(Integer userId, String title, String content) {
        if (userId == null) {
            return;
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Bildirim gönderilecek kullanıcı (ID: " + userId + ") veritabanında bulunamadı."));
        saveNotification(user, title, content);
    }

    private void saveNotification(UserEntity user, String title, String content) {
        NotificationEntity bildirim = new NotificationEntity();
        bildirim.setUser(user);
        bildirim.setTitle(title);
        bildirim.setContent(content);
        bildirim.setIsRead(0);
        bildirim.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(bildirim);
    }
}