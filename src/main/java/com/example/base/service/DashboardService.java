package com.example.base.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.PrioritizationEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.repository.PrioritizationRepository;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.WorkflowRepository;

@Service
public class DashboardService {

    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;
    private final PrioritizationRepository prioritizationRepository;

    public DashboardService(RequestRepository requestRepository,
                             WorkflowRepository workflowRepository,
                             PrioritizationRepository prioritizationRepository) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.prioritizationRepository = prioritizationRepository;
    }

    public long getBekleyenTalepSayisi() {
        return requestRepository.countByStatus("NEW");
    }

    public long getIncelemedekiTalepSayisi() {
        return requestRepository.countByStatus("İncelemede");
    }

    public long getIsAkisinaDonusenTalepSayisi() {
        return requestRepository.countByStatus("İş Akışına Dönüştü");
    }

    public long getBacklogGorevSayisi() {
        return workflowRepository.countByWorkflowStatus("BACKLOG");
    }

    
    @Transactional(readOnly = true)
public List<RequestEntity> getEnYuksekOncelikliIlk5Talep() {
    List<PrioritizationEntity> top5 = prioritizationRepository.findTop5ByOrderByPriorityScoreDesc();

    List<RequestEntity> result = new ArrayList<>();
    for (PrioritizationEntity p : top5) {
        RequestEntity request = p.getRequest();
        Hibernate.initialize(request);
        result.add(request);
    }
    return result;
}

    public long getAcilMudahaleBekleyenSayisi() {
        return prioritizationRepository.countByUrgency(4);
    }
}