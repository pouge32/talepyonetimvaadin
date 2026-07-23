package com.example.base.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_prioritizations")
public class PrioritizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "priority_id")
    private Integer priorityId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_priority_request"))
    private RequestEntity request;

    @Column(name = "urgency")
    private Integer urgency;

    @Column(name = "impact")
    private Integer impact;

    @Column(name = "priority_score")
    private Integer priorityScore;

    @Column(name = "effort")
    private Integer effort;

    @Column(name = "is_security_override")
    private Integer isSecurityOverride = 0;

    public PrioritizationEntity() {
    }

    public PrioritizationEntity(Integer priorityId, RequestEntity request, Integer urgency, Integer impact,
                           Integer priorityScore, Integer effort, Integer isSecurityOverride) {
        this.priorityId = priorityId;
        this.request = request;
        this.urgency = urgency;
        this.impact = impact;
        this.priorityScore = priorityScore;
        this.effort = effort;
        this.isSecurityOverride = isSecurityOverride;
    }

    public Integer getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(Integer priorityId) {
        this.priorityId = priorityId;
    }

    public RequestEntity getRequest() {
        return request;
    }

    public void setRequest(RequestEntity request) {
        this.request = request;
    }

    public Integer getUrgency() {
        return urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getImpact() {
        return impact;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Integer getEffort() {
        return effort;
    }

    public void setEffort(Integer effort) {
        this.effort = effort;
    }

    public Integer getIsSecurityOverride() {
        return isSecurityOverride;
    }

    public void setIsSecurityOverride(Integer isSecurityOverride) {
        this.isSecurityOverride = isSecurityOverride;
    }
}