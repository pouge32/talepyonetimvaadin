package com.example.base.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_sub_tasks")
public class SubTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sub_task_seq_gen")
    @SequenceGenerator(name = "sub_task_seq_gen", sequenceName = "sub_task_sequence", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowEntity workflow;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    public SubTaskEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public WorkflowEntity getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowEntity workflow) { this.workflow = workflow; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}