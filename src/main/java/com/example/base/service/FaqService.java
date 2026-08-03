package com.example.base.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.base.entity.FaqEntity;
import com.example.base.repository.FaqRepository;

import jakarta.annotation.PostConstruct;

@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @PostConstruct
    public void initDummies() {
        if(faqRepository.count() == 0) {
            faqRepository.save(new FaqEntity("Şifremi unuttum, nasıl sıfırlayabilirim?", "Giriş ekranındaki 'Şifremi Unuttum' bağlantısına tıklayarak kayıtlı e-posta adresinize sıfırlama linki gönderebilirsiniz."));
            faqRepository.save(new FaqEntity("Sistem çok yavaş çalışıyor / Rapor alamıyorum", "Eğer excel veya PDF raporu alırken donma yaşıyorsanız, lütfen rapor tarih aralığını 1 aya düşürerek tekrar deneyin. Sunucu yoğunluğuna göre işlem 1-2 dakika sürebilir."));
            faqRepository.save(new FaqEntity("Faturamı / Belgemi nasıl indirebilirim?", "Profil ayarları > Belgelerim sekmesinden geçmiş döneme ait dosyalarınızı PDF olarak cihazınıza indirebilirsiniz."));
            faqRepository.save(new FaqEntity("VPN ile sisteme bağlanamıyorum", "Sistemimiz güvenlik gereği yurtdışı IP'lerine kapalıdır. Kurumsal VPN'i kapatıp normal internet bağlantınız ile girmeyi deneyin."));
        }
    }

    public List<FaqEntity> searchFaq(String keyword) {
        if(keyword == null || keyword.isEmpty()) return faqRepository.findAll();
        return faqRepository.findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(keyword, keyword);
    }
}