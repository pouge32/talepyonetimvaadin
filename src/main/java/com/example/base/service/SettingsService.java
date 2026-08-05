package com.example.base.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.base.entity.CategoryEntity;
import com.example.base.entity.SystemSettingEntity;
import com.example.base.repository.CategoryRepository;
import com.example.base.repository.SystemSettingRepository;

import jakarta.annotation.PostConstruct; 

@Service
public class SettingsService {

    public static final String SLA_LIMIT_HOURS = "sla_limit_hours";
    public static final String SLA_WARNING_PERCENT = "sla_warning_percent";

    private final SystemSettingRepository settingRepository;
    private final CategoryRepository categoryRepository;

    public SettingsService(SystemSettingRepository settingRepository, CategoryRepository categoryRepository) {
        this.settingRepository = settingRepository;
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    public void initDefaultCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new CategoryEntity("Yazılım Hatası"));
            categoryRepository.save(new CategoryEntity("Donanım"));
            categoryRepository.save(new CategoryEntity("Ağ / Bağlantı"));
            categoryRepository.save(new CategoryEntity("Erişim / Yetki Talebi"));
            categoryRepository.save(new CategoryEntity("Diğer"));
        }
    }


    public long getSlaLimitHours() {
        return settingRepository.findBySettingKey(SLA_LIMIT_HOURS)
                .map(s -> Long.parseLong(s.getSettingValue()))
                .orElse(24L);
    }

    public double getSlaWarningPercent() {
        return settingRepository.findBySettingKey(SLA_WARNING_PERCENT)
                .map(s -> Double.parseDouble(s.getSettingValue()))
                .orElse(0.75);
    }

    @Transactional
    public void updateSlaSettings(long limitHours, double warningPercent) {
        upsert(SLA_LIMIT_HOURS, String.valueOf(limitHours), "SLA ihlal süresi (saat)");
        upsert(SLA_WARNING_PERCENT, String.valueOf(warningPercent), "SLA uyarı eşiği (0-1 arası oran)");
    }

    private void upsert(String key, String value, String description) {
        SystemSettingEntity setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> new SystemSettingEntity(key, value, description));
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }


    public List<CategoryEntity> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }
    
    public List<CategoryEntity> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<String> getActiveCategoryNames() {
        return getActiveCategories().stream().map(CategoryEntity::getName).collect(Collectors.toList());
    }

    @Transactional
    public CategoryEntity addCategory(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Bu kategori zaten mevcut: " + name);
        }
        return categoryRepository.save(new CategoryEntity(name));
    }

    @Transactional
    public void setCategoryActive(Integer categoryId, boolean active) {
        categoryRepository.findById(categoryId).ifPresent(c -> {
            c.setActive(active);
            categoryRepository.save(c);
        });
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}