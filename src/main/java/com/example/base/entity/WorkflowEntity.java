package com.example.base.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_workflows")
public class WorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_request"))
    private RequestEntity request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", foreignKey = @ForeignKey(name = "fk_developer"))
    private UserEntity developer;

    
    @Column(name = "workflow_status", length = 30)
    private String workflowStatus = "BACKLOG";

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();

    public WorkflowEntity() {
    }

    public WorkflowEntity(Integer taskId, RequestEntity request, UserEntity developer,
                     String workflowStatus, LocalDateTime assignedAt) {
        this.taskId = taskId;
        this.request = request;
        this.developer = developer;
        this.workflowStatus = workflowStatus;
        this.assignedAt = assignedAt;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public RequestEntity getRequest() {
        return request;
    }

    public void setRequest(RequestEntity request) {
        this.request = request;
    }

    public UserEntity getDeveloper() {
        return developer;
    }

    public void setDeveloper(UserEntity developer) {
        this.developer = developer;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}