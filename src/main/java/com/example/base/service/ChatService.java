package com.example.base.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.MessageEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.MessageRepository;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ChatService(MessageRepository messageRepository,
                        RequestRepository requestRepository,
                        UserRepository userRepository,
                        NotificationService notificationService
                    ) {
        this.messageRepository = messageRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> getMessages(Integer requestId) {
        return messageRepository.findByRequest_RequestIdOrderBySentAtAsc(requestId);
    }

    @Transactional
    public MessageEntity sendMessage(Integer requestId, Integer senderId, Integer receiverId, String messageBody) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Mesaj boş olamaz.");
        }

        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Talep bulunamadı: " + requestId));
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalStateException("Gönderen kullanıcı bulunamadı: " + senderId));
        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalStateException("Alıcı kullanıcı bulunamadı: " + receiverId));

        MessageEntity message = new MessageEntity(request, sender, receiver, messageBody.trim());
        MessageEntity saved = messageRepository.save(message);

        ChatBroadcaster.broadcast(requestId, saved);
        notificationService.notifyUser(receiverId, "Yeni Mesaj", "Talep detayında yeni bir mesajınız var!");

        return saved;
    }

    /**
     * Bir talepte, verilen kullanıcının "karşı tarafı" kimdir?
     * Müşteri konuşuyorsa karşı taraf destek/PO tarafındaki en son mesajlaşılan kişi;
     * yoksa (henüz kimse yazmadıysa) talebin sahibi müşteridir.
     */
    @Transactional(readOnly = true)
    public UserEntity resolveOtherParty(Integer requestId, Integer currentUserId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Talep bulunamadı: " + requestId));

        UserEntity customer = request.getCustomer();
        if (customer != null && !customer.getUserId().equals(currentUserId)) {
            return customer;
        }

        List<MessageEntity> history = getMessages(requestId);
        for (MessageEntity m : history) {
            if (!m.getSender().getUserId().equals(currentUserId)) {
                return m.getSender();
            }
            if (!m.getReceiver().getUserId().equals(currentUserId)) {
                return m.getReceiver();
            }
        }

        throw new IllegalStateException(
                "Bu talep için karşı taraf belirlenemedi — henüz personel ataması yapılmamış olabilir.");
    }

    
    public int getUnreadMessageCount(Integer requestId, Integer userId) {
        return messageRepository.countByRequest_RequestIdAndReceiver_UserIdAndIsRead(requestId, userId, 0);
    }

    @Transactional
    public void markMessagesAsRead(Integer requestId, Integer userId) {
        List<MessageEntity> unreadMessages = messageRepository.findByRequest_RequestIdAndReceiver_UserIdAndIsRead(requestId, userId, 0);
        for (MessageEntity msg : unreadMessages) {
            msg.setIsRead(1);
        }
        messageRepository.saveAll(unreadMessages);
    }
}