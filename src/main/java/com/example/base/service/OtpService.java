package com.example.base.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;

@Service
public class OtpService {

    private final UserRepository userRepository;

    public OtpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void generateAndSendOtp(UserEntity user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        userRepository.save(user);

        mockSendEmail(user.getEmail(), otp);
    }

    public boolean validateOtp(UserEntity user, String inputCode) {
        if (user.getOtpCode() == null || user.getOtpExpiry() == null) return false;
        
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return false;
        }
        
        boolean isValid = user.getOtpCode().equals(inputCode);
        if (isValid) {
            user.setOtpCode(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
        }
        return isValid;
    }

    private void mockSendEmail(String to, String otp) {
        System.out.println("\n========================================================");
        System.out.println("  [MOCK MAİL SERVİSİ] - 2FA MAİLİ GÖNDERİLDİ");
        System.out.println("========================================================");
        System.out.println("  Alıcı E-Posta : " + to);
        System.out.println("  Konu          : Sistem Girişi - 2FA Doğrulama Kodu");
        System.out.println("  Mesaj         : Talep Yönetim Sistemine yetkili giriş denemesi tespit edildi.");
        System.out.println("                  Lütfen aşağıdaki kodu ekrana giriniz.");
        System.out.println("  ");
        System.out.println("  GÜVENLİK KODU : " + otp);
        System.out.println("========================================================\n");
    }
}