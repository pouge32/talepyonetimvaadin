package com.example.base.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.PrioritizationEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.PrioritizationRepository;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RequestService {
    
    private final RequestRepository requestRepository;
    private final PrioritizationRepository prioritizationRepository;
    private final UserRepository userRepository;
    private final WorkflowRepository workflowRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;

    public RequestService(RequestRepository requestRepository,
                           PrioritizationRepository prioritizationRepository,
                           UserRepository userRepository,
                           WorkflowRepository workflowRepository,
                           NotificationService notificationService,
                           SystemLogService systemLogService,
                           SettingsService settingsService) {
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
        this.userRepository = userRepository;
        this.workflowRepository = workflowRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
    }

    public List<RequestEntity> getMyRequestsForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));
        return requestRepository.findByCustomer_UserId(customer.getUserId());
    }

    @Transactional
    public RequestEntity createRequest(Integer customerId, String title, String description, String category) {
        return createRequest(customerId, title, description, category, null, null, null);
    }

    @Transactional
    public RequestEntity createRequest(Integer customerId, String title, String description, String category,
                                        byte[] screenshotData, String screenshotFileName, String screenshotMimeType) {
        UserEntity customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + customerId));

        RequestEntity request = new RequestEntity();
        request.setCustomer(customer);
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        request.setStatus("NEW");
        request.setCreatedAt(LocalDateTime.now());

        if (screenshotData != null && screenshotData.length > 0) {
            request.setScreenshotData(screenshotData);
            request.setScreenshotFileName(screenshotFileName);
            request.setScreenshotMimeType(screenshotMimeType);
        }

        RequestEntity savedRequest = requestRepository.save(request);
        autoAssignRequest(savedRequest);
        return savedRequest;
    }

    @Transactional
    public void autoAssignRequest(RequestEntity request) {
        List<UserEntity> helpdeskUsers = userRepository.findByRole(Role.HELPDESK);
        if (helpdeskUsers.isEmpty()) {
            return; 
        }

        UserEntity leastLoadedUser = null;
        long minLoad = Long.MAX_VALUE;
        List<String> activeStatuses = Arrays.asList("NEW", "INCELEMEDE");

        for (UserEntity user : helpdeskUsers) {
            long currentLoad = requestRepository.countByAssignedUserAndStatusIn(user, activeStatuses);
            if (currentLoad < minLoad) {
                minLoad = currentLoad;
                leastLoadedUser = user;
            }
        }

        if (leastLoadedUser != null) {
            request.setAssignedUser(leastLoadedUser);
            requestRepository.save(request);
            notificationService.notifyUser(leastLoadedUser.getUserId(), 
                "Sistem Tarafından Yeni Görev Atandı", 
                "Yük dengeleme algoritması tarafından #" + request.getRequestId() + " numaralı talep size zimmetlenmiştir.");
        }
    }

    public List<RequestEntity> getMyRequests(Integer customerId, boolean lastWeekOnly) {
        if (lastWeekOnly) {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            return requestRepository.findByCustomer_UserIdAndCreatedAtAfter(customerId, oneWeekAgo);
        }
        return requestRepository.findByCustomer_UserId(customerId);
    }

    @Transactional
    public PrioritizationEntity prioritizeRequest(Integer requestId, int urgency, int impact, int effort, boolean isSecurityOverride) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Talep bulunamadı: " + requestId));

        PrioritizationEntity prioritization = prioritizationRepository
                .findByRequest_RequestId(requestId)
                .orElseGet(PrioritizationEntity::new);

        prioritization.setRequest(request);
        prioritization.setUrgency(urgency);
        prioritization.setImpact(impact);
        prioritization.setEffort(effort); 
        prioritization.setIsSecurityOverride(isSecurityOverride ? 1 : 0);

        int score = calculateScore(urgency, impact, effort, isSecurityOverride);
        prioritization.setPriorityScore(score);

        int threshold = settingsService.getPoAutoApprovalThreshold();
        if (score >= threshold || isSecurityOverride) {
            request.setStatus("ONAYLANDI");
        } else {
            request.setStatus("INCELEMEDE");
        }
        requestRepository.save(request);

        return prioritizationRepository.save(prioritization);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void applyAgingRule() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        List<RequestEntity> staleRequests = requestRepository
                .findByStatusAndCreatedAtBefore("NEW", threshold);

        for (RequestEntity request : staleRequests) {
            prioritizationRepository.findByRequest_RequestId(request.getRequestId())
                    .ifPresent(p -> {
                        boolean isOverridden = p.getIsSecurityOverride() != null && p.getIsSecurityOverride() == 1;
                        boolean canIncrease = p.getUrgency() != null && p.getUrgency() < 4;
                        if (!isOverridden && canIncrease) {
                            p.setUrgency(p.getUrgency() + 1);
                            int currentEffort = p.getEffort() != null ? p.getEffort() : 1;
                            p.setPriorityScore(calculateScore(p.getUrgency(), p.getImpact(), currentEffort, false));
                            prioritizationRepository.save(p);
                        }
                    });
        }
    }

    public int calculateScore(int urgency, int impact, int effort, boolean securityOverride) {
        if (securityOverride) {
            return 999;
        }
        float value = (impact * 2.0f) * urgency;
        return Math.round(value / effort);
    }

    public String getPriorityLabel(int score) {
        if (score >= 999) return "ACİL / GÜVENLİK"; 
        if (score >= 20)  return "KRİTİK";         
        if (score >= 10)  return "YÜKSEK";         
        if (score >= 5)   return "ORTA"; 
        return "DÜŞÜK";                               
    }

    public List<RequestEntity> getNewRequests() {
        return requestRepository.findByStatus("NEW");
    }

    public Integer getFirstCustomerId() {
        return userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Veritabanında kullanıcı bulunamadı! Lütfen PL/SQL'den bir kullanıcı ekleyin."))
                .getUserId();
    }

    public String getRequestPriority(Integer requestId) {
        return prioritizationRepository.findByRequest_RequestId(requestId)
                .map(p -> p.getPriorityScore() + " - " + getPriorityLabel(p.getPriorityScore()))
                .orElse("Puan Bekliyor"); 
    }

    @Transactional
    public WorkflowEntity goreveDonustur(RequestEntity talep) {
        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setRequest(talep);
        workflow.setWorkflowStatus("BACKLOG");
        workflow.setAssignedAt(LocalDateTime.now());

        WorkflowEntity saved = workflowRepository.save(workflow);

        talep.setStatus("İş Akışına Dönüştü");
        requestRepository.save(talep);

        return saved;
    }

    @Transactional
    public void reopenRequest(Integer requestId, String reason) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Talep bulunamadı: " + requestId));

        if (!"KAPATILDI".equals(request.getStatus())) {
            throw new IllegalStateException("Sadece kapatılmış talepler yeniden açılabilir.");
        }

        request.setStatus("NEW"); 
        request.setAssignedUser(null);
        requestRepository.save(request);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        systemLogService.log("Müşteri (" + email + "), #" + requestId + " numaralı talebi YENİDEN AÇTI. Gerekçe: " + reason);
        notificationService.notifyRole("HELPDESK", "Talep Yeniden Açıldı", "#" + requestId + " numaralı talep müşteri tarafından çözülmediği gerekçesiyle yeniden açıldı.");

        autoAssignRequest(request); 
    }

    @Transactional
    public void rateRequest(Integer requestId, int score, String comment) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Talep bulunamadı: " + requestId));

        request.setStatus("KAPATILDI");
        request.setSatisfactionScore(score);
        request.setSatisfactionComment(comment);
        requestRepository.save(request);

        systemLogService.log("Talep ID: " + requestId + " müşteri tarafından " + score + " yıldız ile değerlendirildi.");

        if (score <= 2) {
            String staffName = request.getAssignedUser() != null ? request.getAssignedUser().getNameSurname() : "Atanmamış/Bilinmiyor";
            String staffEmail = request.getAssignedUser() != null ? request.getAssignedUser().getEmail() : "-";
            
            String notifBody = "#" + requestId + " numaralı talepte müşteri " + score + " yıldız verdi. " +
                               "İlgilenen Personel: " + staffName + " (" + staffEmail + "). " +
                               "Müşteri Notu: " + (comment != null && !comment.isEmpty() ? comment : "Belirtilmedi");

            systemLogService.log("DÜŞÜK PUAN UYARISI : #" + requestId + " talebinde " + staffName + " personeline " + score + " yıldız verildi.");
            notificationService.notifyRole("ADMIN", "Düşük Memnuniyet Uyarısı ", notifBody);
        }
    }

    public boolean hasSimilarOpenRequest(String title, String description) {
        List<RequestEntity> openRequests = requestRepository.findAll().stream()
                .filter(req -> !"KAPATILDI".equals(req.getStatus()))
                .toList();

        String newText = (title + " " + description).toLowerCase();

        for (RequestEntity req : openRequests) {
            String existingText = (req.getTitle() + " " + req.getDescription()).toLowerCase();
            if (calculateTextSimilarity(newText, existingText) > 0.40) {
                systemLogService.log("Benzer talep tespit edildi. Mevcut Talep ID: " + req.getRequestId());
                return true;
            }
        }
        return false;
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;

        List<String> words1 = java.util.Arrays.stream(text1.split("\\W+")).filter(w -> w.length() > 2).toList();
        List<String> words2 = java.util.Arrays.stream(text2.split("\\W+")).filter(w -> w.length() > 2).toList();

        if (words1.isEmpty() || words2.isEmpty()) return 0.0;

        java.util.Set<String> set1 = new java.util.HashSet<>(words1);
        java.util.Set<String> set2 = new java.util.HashSet<>(words2);

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    @Transactional
    public List<RequestEntity> getAllRequestsForGrid() {
        List<RequestEntity> requests = requestRepository.findAll();
        
        requests.forEach(req -> {
            if (req.getAssignedUser() != null) {
                req.getAssignedUser().getRole(); 
                req.getAssignedUser().getNameSurname();
            }
            if (req.getCustomer() != null) {
                req.getCustomer().getNameSurname();
            }
            if (req.getPrioritization() != null) {
                req.getPrioritization().getPriorityScore();
            }
        });
        
        return requests;
    }
}