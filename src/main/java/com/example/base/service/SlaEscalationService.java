package com.example.base.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;

@Service
public class SlaEscalationService {

    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;

    public SlaEscalationService(RequestRepository requestRepository, 
                                NotificationService notificationService, 
                                SystemLogService systemLogService) {
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
    }

    @Scheduled(fixedRate = 1800000) 
    public void checkSlaViolations() {
        List<RequestEntity> openRequests = requestRepository.findAll().stream()
                .filter(req -> !"KAPATILDI".equals(req.getStatus()))
                .toList();

        LocalDateTime now = LocalDateTime.now();

        for (RequestEntity request : openRequests) {
            if (request.getCreatedAt() != null) {
                long hoursElapsed = ChronoUnit.HOURS.between(request.getCreatedAt(), now);

                long slaLimit = 24; 
                long warningLimit = 18; 

                if (hoursElapsed == slaLimit) {
                    systemLogService.log("OTOMATİK ALARM: Talep ID " + request.getRequestId() + " için SLA süresi aşıldı! (" + slaLimit + " Saat)");
                    
                } 
                else if (hoursElapsed == warningLimit) {
                    systemLogService.log("SLA UYARISI: Talep ID " + request.getRequestId() + " için SLA ihlaline yaklaşıldı. Geçen süre: " + hoursElapsed + " Saat.");
                }
            }
        }
    }
}