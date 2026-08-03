package com.example.base.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "XAY_SYSTEM_LOGS")
public class SystemLogEntity {

    @Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "log_seq")
@SequenceGenerator(name = "log_seq", sequenceName = "XAY_SYSTEM_LOGS_SEQ", allocationSize = 1)
@Column(name = "LOG_ID")
private Long logId;

    @Column(name = "ACTION", length = 1000)
    private String action;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action; 
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}