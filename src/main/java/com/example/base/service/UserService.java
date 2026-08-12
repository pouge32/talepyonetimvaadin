package com.example.base.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;

    public UserService(UserRepository userRepository, 
                       NotificationService notificationService, 
                       SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
    }

    @Transactional
    public UserEntity registerCustomer(String nameSurname, String email, String passwordHash) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Bu email zaten kayıtlı: " + email);
        }
        UserEntity user = new UserEntity();
        user.setNameSurname(nameSurname);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.CUSTOMER);
        user.setRegistrationStatus("PENDING");
        return userRepository.save(user);
    }

    public List<UserEntity> getPendingRegistrations() {
        return userRepository.findByRegistrationStatus("PENDING");
    }

    @Transactional
    public UserEntity approveRegistration(Integer userId) {
        UserEntity user = getUserOrThrow(userId);
        user.setRegistrationStatus("APPROVED");
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity rejectRegistration(Integer userId) {
        UserEntity user = getUserOrThrow(userId);
        user.setRegistrationStatus("REJECTED");
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateProfile(Integer userId, String nameSurname, String profilePhotoUrl) {
        UserEntity user = getUserOrThrow(userId);
        if (nameSurname != null) {
            user.setNameSurname(nameSurname);
        }
        if (profilePhotoUrl != null) {
            user.setProfilePhotoUrl(profilePhotoUrl);
        }
        return userRepository.save(user);
    }

    private UserEntity getUserOrThrow(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + userId));
    }

    @Transactional
    public void requestAccountDeletion(Integer userId) {
        UserEntity user = getUserOrThrow(userId);
        
        user.setDeletionRequested(true);
        user.setDeletionRequestDate(LocalDateTime.now());
        userRepository.save(user);

        systemLogService.log("KVKK İHLAL/SİLME TALEBİ: Müşteri (" + user.getEmail() + ") 6698 sayılı kanun kapsamında hesabının silinmesini talep etti.");

        notificationService.notifyRole("PO", 
            "KVKK Veri Silme Talebi", 
            user.getNameSurname() + " isimli kullanıcı hesap verilerinin silinmesini talep ediyor. Onay bekliyor.");
    }

    @Transactional
    public void approveKvkkDeletion(Integer userId) {
        UserEntity user = getUserOrThrow(userId);
        
        user.setNameSurname("Anonim Kullanıcı (" + user.getUserId() + ")");
        user.setEmail("deleted_" + user.getUserId() + "@anonymized.local");
        user.setPasswordHash("DELETED_ACCOUNT");
        user.setProfilePhotoUrl(null);
        
        user.setDeletionRequested(false); 
        user.setBanned(true);
        
        userRepository.save(user);

        systemLogService.log("KVKK ONAY: Yöneticiler " + userId + " ID'li kullanıcının verilerini anonimleştirerek sistemi yasalara uygun hale getirdi.");
    }

    @Transactional
    public void rejectKvkkDeletion(Integer userId) {
        UserEntity user = getUserOrThrow(userId);
        
        user.setDeletionRequested(false);
        user.setDeletionRequestDate(null);
        userRepository.save(user);

        systemLogService.log("KVKK RED: " + user.getEmail() + " adlı kullanıcının hesap silme talebi reddedildi.");
    }

    public List<UserEntity> getPendingDeletionRequests() {
        return userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getDeletionRequested()))
                .toList();
    }
}