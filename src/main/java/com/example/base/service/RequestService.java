package com.example.base.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.PrioritizationEntity;
import com.example.base.entity.RequestEntity;
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
    public List<RequestEntity> getMyRequestsForCurrentUser() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();

    UserEntity customer = userRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));

    return requestRepository.findByCustomer_UserId(customer.getUserId());
    }

    public RequestService(RequestRepository requestRepository,
                           PrioritizationRepository prioritizationRepository,
                           UserRepository userRepository,
                           WorkflowRepository workflowRepository
                        ) {
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
        this.userRepository = userRepository;
        this.workflowRepository = workflowRepository;
        
    }

    @Transactional
    public RequestEntity createRequest(Integer customerId, String title, String description) {
        UserEntity customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + customerId));

        RequestEntity request = new RequestEntity();
        request.setCustomer(customer);
        request.setTitle(title);
        request.setDescription(description);
        request.setStatus("NEW");
        request.setCreatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    public List<RequestEntity> getMyRequests(Integer customerId, boolean lastWeekOnly) {
        if (lastWeekOnly) {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            return requestRepository.findByCustomer_UserIdAndCreatedAtAfter(customerId, oneWeekAgo);
        }
        return requestRepository.findByCustomer_UserId(customerId);
    }

    
    @Transactional
public PrioritizationEntity prioritizeRequest(Integer requestId, int urgency, int impact) {
    RequestEntity request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Talep bulunamadı: " + requestId));

    PrioritizationEntity prioritization = prioritizationRepository
            .findByRequest_RequestId(requestId)
            .orElseGet(PrioritizationEntity::new);

    prioritization.setRequest(request);
    prioritization.setUrgency(urgency);
    prioritization.setImpact(impact);
    prioritization.setEffort(1); 
    prioritization.setIsSecurityOverride(0);

    int score = calculateScore(urgency, impact, false);
    prioritization.setPriorityScore(score);

    if (score >= 8) {
        request.setStatus("Onaylandı");
    } else {
        request.setStatus("İncelemede");
    }
    requestRepository.save(request);

    return prioritizationRepository.save(prioritization);
}

    
    @Transactional
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
                            p.setPriorityScore(calculateScore(p.getUrgency(), p.getImpact(), false));
                            prioritizationRepository.save(p);
                        }
                    });
        }
    }

    private int calculateScore(int urgency, int impact, boolean securityOverride) {
        if (securityOverride) {
            return 16;
        }
        return urgency * impact;
    }

    public String getPriorityLabel(int score) {
        if (score >= 12) return "KRİTİK";
        if (score >= 8) return "YÜKSEK";
        if (score >= 3) return "ORTA";
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
}