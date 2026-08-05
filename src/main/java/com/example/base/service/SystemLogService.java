package com.example.base.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.base.entity.SystemLogEntity;
import com.example.base.repository.SystemLogRepository;

@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    public void log(String action) {
        try {
            SystemLogEntity logEntity = new SystemLogEntity();
            logEntity.setAction(action);
            logEntity.setCreatedAt(LocalDateTime.now());
            systemLogRepository.save(logEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SystemLogEntity> getLogsForRequest(Integer requestId) {
        return systemLogRepository.findByActionContaining("ID: " + requestId);
    }
}