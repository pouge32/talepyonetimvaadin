package com.example.base.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.NotificationRepository;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.SystemLogRepository;
import com.example.base.repository.UserRepository;

@Service
public class DemoDataService {

    private final RequestRepository requestRepository;
    private final SystemLogRepository systemLogRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public DemoDataService(RequestRepository requestRepository, 
                           SystemLogRepository systemLogRepository,
                           NotificationRepository notificationRepository, 
                           UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.systemLogRepository = systemLogRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void resetSystemForDemo() {
        notificationRepository.deleteAll();
        systemLogRepository.deleteAll();
        requestRepository.deleteAll();

        UserEntity sampleCustomer = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("CUSTOMER"))
                .findFirst()
                .orElse(null);

        if (sampleCustomer != null) {
            createDummyRequest("Sunucu Bağlantı Hatası", "Ana veritabanı sunucusuna bağlanırken 'Timeout' hatası alıyoruz. Acil kontrol edilmeli.", "NEW", sampleCustomer);
            createDummyRequest("UI Düzenlenmesi (Tasarım)", "Müşteri panelindeki butonların renkleri kurumsal kimliğe uymuyor. Renk kodları güncellenmeli.", "INCELEMEDE", sampleCustomer);
            createDummyRequest("Yeni Raporlama Ekranı", "Yöneticiler için aylık performansları gösteren yeni bir PDF rapor modülü isteniyor.", "ONAYLANDI", sampleCustomer);
        }
    }

    private void createDummyRequest(String title, String desc, String status, UserEntity customer) {
        RequestEntity req = new RequestEntity();
        req.setTitle(title);
        req.setDescription(desc);
        req.setStatus(status);
        req.setCustomer(customer);
        req.setCreatedAt(LocalDateTime.now());
        requestRepository.save(req);
    }
}