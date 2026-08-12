package com.example.base.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.GlobalChatMessageEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.GlobalChatMessageRepository;
import com.example.base.repository.UserRepository;

@Service
public class GlobalChatService {

    private final GlobalChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final GlobalChatBroadcaster broadcaster;

    public GlobalChatService(GlobalChatMessageRepository messageRepo, UserRepository userRepo, GlobalChatBroadcaster broadcaster) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.broadcaster = broadcaster;
    }

    public List<GlobalChatMessageEntity> getAllMessages() {
        return messageRepo.findAllWithSenderAndReads().stream().distinct().collect(Collectors.toList());
    }

    public List<GlobalChatMessageEntity> getVisibleMessagesForUser(Integer userId) {
        List<GlobalChatMessageEntity> all = getAllMessages();
        return all.stream().filter(msg -> {
            if (msg.getReceiver() == null) return true;
            return msg.getSender().getUserId().equals(userId) || msg.getReceiver().getUserId().equals(userId);
        }).collect(Collectors.toList());
    }

    @Transactional
    public GlobalChatMessageEntity sendMessage(Integer senderId, String text, byte[] fileData, String fileName) {
        UserEntity sender = userRepo.findById(senderId).orElseThrow();
        String content = text != null ? text : "";
        UserEntity receiver = null;

        if (content.toLowerCase().startsWith("/msg ")) {
            List<UserEntity> allUsers = userRepo.findAll();
            for (UserEntity u : allUsers) {
                String namePrefix = "/msg " + u.getNameSurname();
                String rolePrefix = u.getRole() != null ? "/msg " + u.getRole().name() : "";

                if (content.regionMatches(true, 0, namePrefix, 0, namePrefix.length())) {
                    receiver = u;
                    content = content.substring(namePrefix.length()).trim();
                    break;
                } else if (!rolePrefix.isEmpty() && content.regionMatches(true, 0, rolePrefix, 0, rolePrefix.length())) {
                    receiver = u;
                    content = content.substring(rolePrefix.length()).trim();
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

        GlobalChatMessageEntity message = new GlobalChatMessageEntity();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setFileData(fileData);
        message.setFileName(fileName);
        
        message.getReadByUsers().add(sender);

        messageRepo.save(message);
        broadcaster.broadcast(message);
        return message;
    }

    public int getUnreadCountForUser(Integer userId) {
        List<GlobalChatMessageEntity> messages = getVisibleMessagesForUser(userId);
        int count = 0;
        for (GlobalChatMessageEntity msg : messages) {
            boolean read = msg.getReadByUsers().stream().anyMatch(u -> u.getUserId().equals(userId));
            if (!read) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public void markAllAsReadForUser(Integer userId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        List<GlobalChatMessageEntity> messages = getVisibleMessagesForUser(userId);
        boolean updated = false;
        for (GlobalChatMessageEntity msg : messages) {
            if (!msg.getReadByUsers().contains(user)) {
                msg.getReadByUsers().add(user);
                updated = true;
            }
        }
        if (updated) {
            messageRepo.saveAll(messages);
        }
    }
}