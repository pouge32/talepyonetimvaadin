package com.example.base.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_internal_comments")
public class InternalCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "int_comment_seq_gen")
    @SequenceGenerator(name = "int_comment_seq_gen", sequenceName = "internal_comment_sequence", allocationSize = 1)
    private Integer commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestEntity request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private UserEntity receiver;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Lob
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "file_name", length = 255)
    private String fileName;

    public InternalCommentEntity() {}

    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }

    public RequestEntity getRequest() { return request; }
    public void setRequest(RequestEntity request) { this.request = request; }

    public UserEntity getSender() { return sender; }
    public void setSender(UserEntity sender) { this.sender = sender; }

    public UserEntity getReceiver() { return receiver; }
    public void setReceiver(UserEntity receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}