package com.example.base.entity;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_requests")
public class RequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "req_seq_gen")
    @SequenceGenerator(name = "req_seq_gen", sequenceName = "request_sequence", allocationSize = 1)
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_customer"))
    private UserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", foreignKey = @ForeignKey(name = "fk_assigned_user"))
    private UserEntity assignedUser;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "status", length = 30)
    private String status = "NEW";

    @Column(name = "category", length = 50)
    private String category = "Diğer";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Lob
    @Column(name = "screenshot_data")
    private byte[] screenshotData;

    @Column(name = "screenshot_filename", length = 255)
    private String screenshotFileName;

    @Column(name = "screenshot_mime_type", length = 100)
    private String screenshotMimeType;

    @OneToOne(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private PrioritizationEntity prioritization;

    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;

    @Column(name = "satisfaction_comment", length = 500)
    private String satisfactionComment;

    @Column(name = "survey_completed")
    private Boolean surveyCompleted = false;

    @Column(name = "survey_reminder_sent")
    private Boolean surveyReminderSent = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RequestEntity() {
    }

    public RequestEntity(Integer requestId, UserEntity customer, String title, String description,
                    String status, LocalDateTime createdAt, PrioritizationEntity prioritization) {
        this.requestId = requestId;
        this.customer = customer;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.prioritization = prioritization;
    }

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }

    public UserEntity getCustomer() { return customer; }
    public void setCustomer(UserEntity customer) { this.customer = customer; }

    public UserEntity getAssignedUser() { return assignedUser; }
    public void setAssignedUser(UserEntity assignedUser) { this.assignedUser = assignedUser; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public PrioritizationEntity getPrioritization() { return prioritization; }
    public void setPrioritization(PrioritizationEntity prioritization) { this.prioritization = prioritization; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public byte[] getScreenshotData() { return screenshotData; }
    public void setScreenshotData(byte[] screenshotData) { this.screenshotData = screenshotData; }

    public String getScreenshotFileName() { return screenshotFileName; }
    public void setScreenshotFileName(String screenshotFileName) { this.screenshotFileName = screenshotFileName; }

    public String getScreenshotMimeType() { return screenshotMimeType; }
    public void setScreenshotMimeType(String screenshotMimeType) { this.screenshotMimeType = screenshotMimeType; }

    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer satisfactionScore) { this.satisfactionScore = satisfactionScore; }

    public String getSatisfactionComment() { return satisfactionComment; }
    public void setSatisfactionComment(String satisfactionComment) { this.satisfactionComment = satisfactionComment; }

    public Boolean getSurveyCompleted() { return surveyCompleted; }
    public void setSurveyCompleted(Boolean surveyCompleted) { this.surveyCompleted = surveyCompleted; }

    public Boolean getSurveyReminderSent() { return surveyReminderSent; }
    public void setSurveyReminderSent(Boolean surveyReminderSent) { this.surveyReminderSent = surveyReminderSent; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}