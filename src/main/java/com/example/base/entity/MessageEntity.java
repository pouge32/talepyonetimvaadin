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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "msg_seq_gen")
    @SequenceGenerator(name = "msg_seq_gen", sequenceName = "message_sequence", allocationSize = 1)
    @Column(name = "message_id")
    private Integer messageId;  

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_request"))
    private RequestEntity request;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserEntity receiver;

    @Lob
    @Column(name = "message_body", nullable = false)
    private String messageBody;

    @Column(name = "sent_at")
    private LocalDateTime sentAt = LocalDateTime.now();

    @Lob
    @Column(name = "file_data")
    private byte[] fileData;

    private String fileName;

    public MessageEntity() {
    }

    public MessageEntity(RequestEntity request, UserEntity sender, UserEntity receiver, String messageBody) {
        this.request = request;
        this.sender = sender;
        this.receiver = receiver;
        this.messageBody = messageBody;
        this.sentAt = LocalDateTime.now();
    }

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }

    public RequestEntity getRequest() { return request; }
    public void setRequest(RequestEntity request) { this.request = request; }

    public UserEntity getSender() { return sender; }
    public void setSender(UserEntity sender) { this.sender = sender; }

    public UserEntity getReceiver() { return receiver; }
    public void setReceiver(UserEntity receiver) { this.receiver = receiver; }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    @Column(name = "is_read")
    private Integer isRead = 0;

    public Integer getIsRead() {return isRead;}

    public void setIsRead(Integer isRead) {this.isRead = isRead;}

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}