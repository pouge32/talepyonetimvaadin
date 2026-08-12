package com.example.base.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;

@Service
public class FeedbackReminderService {

    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;

    public FeedbackReminderService(RequestRepository requestRepository, 
                                   NotificationService notificationService, 
                                   SystemLogService systemLogService) {
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendSurveyReminders() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        
        List<RequestEntity> pendingRequests = requestRepository.findRequestsForSurveyReminder(twentyFourHoursAgo);

        if (!pendingRequests.isEmpty()) {
            for (RequestEntity request : pendingRequests) {
                
                notificationService.notifyUser(
                    request.getCustomer().getUserId(), 
                    "Deneyiminizi Değerlendirin", 
                    "#" + request.getRequestId() + " numaralı talebiniz çözüldü. Hizmet kalitemizi artırmak için lütfen geri bildirim bırakın."
                );

                request.setSurveyReminderSent(true);
                requestRepository.save(request);
                
                systemLogService.log("OTOMATİK İŞLEM: #" + request.getRequestId() + " numaralı talep için müşteriye 24 saatlik anket hatırlatması gönderildi.");
            }
            System.out.println("[SCHEDULER] " + pendingRequests.size() + " adet anket hatırlatması başarıyla gönderildi.");
        }
    }
}