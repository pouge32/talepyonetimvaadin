package com.example.base.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.InternalCommentEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.InternalCommentRepository;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;

@Service
public class InternalCommentService {

    private final InternalCommentRepository commentRepo;
    private final UserRepository userRepo;
    private final RequestRepository requestRepo;
    private final TeamChatBroadcaster broadcaster;

    public InternalCommentService(InternalCommentRepository commentRepo, UserRepository userRepo,
                                  RequestRepository requestRepo, TeamChatBroadcaster broadcaster) {
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
        this.requestRepo = requestRepo;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public InternalCommentEntity sendComment(Integer requestId, Integer senderId, String text) {
        return sendCommentWithFile(requestId, senderId, text, null, null);
    }

    @Transactional
    public InternalCommentEntity sendCommentWithFile(Integer requestId, Integer senderId, String text, byte[] fileData, String fileName) {
        RequestEntity request = requestRepo.findById(requestId).orElseThrow();
        UserEntity sender = userRepo.findById(senderId).orElseThrow();
        
        UserEntity receiver = null;
        String content = text != null ? text : "";

        if (content.toLowerCase().startsWith("/msg ")) {
            List<UserEntity> allUsers = userRepo.findAll();
            for (UserEntity u : allUsers) {
                String prefix = "/msg " + u.getNameSurname();
                if (content.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    receiver = u;
                    content = content.substring(prefix.length()).trim();
                    break;
                }
            }
        }

        if (content.trim().isEmpty()) {
            if (fileData != null && fileData.length > 0) {
                content = "📎 Sadece dosya eki gönderildi.";
            } else {
                content = "-"; 
            }
        }

        InternalCommentEntity comment = new InternalCommentEntity();
        comment.setRequest(request);
        comment.setSender(sender);
        comment.setReceiver(receiver);
        comment.setContent(content);
        comment.setFileData(fileData);
        comment.setFileName(fileName);
        comment.setIsRead(false);

        commentRepo.save(comment);
        broadcaster.broadcast(comment);
        return comment;
    }

    public List<InternalCommentEntity> getVisibleComments(Integer requestId, Integer userId) {
        return commentRepo.findVisibleCommentsForUser(requestId, userId);
    }

    public int getUnreadCountForUser(Integer userId) {
        return commentRepo.countUnreadMessagesForUser(userId);
    }

    @Transactional
    public void markAsRead(Integer requestId, Integer userId) {
        List<InternalCommentEntity> comments = commentRepo.findVisibleCommentsForUser(requestId, userId);
        boolean changed = false;
        for (InternalCommentEntity c : comments) {
            if (!c.getSender().getUserId().equals(userId) && !c.getIsRead()) {
                c.setIsRead(true);
                changed = true;
            }
        }
        if (changed) commentRepo.saveAll(comments);
    }
}