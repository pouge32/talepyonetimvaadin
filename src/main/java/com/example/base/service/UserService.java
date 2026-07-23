package com.example.base.service;

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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}