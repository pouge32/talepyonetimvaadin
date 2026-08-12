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

    public static final String MAINTENANCE_MODE = "maintenance_mode";
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String MAX_FILE_UPLOAD_SIZE_MB = "max_file_upload_size_mb";
    public static final String PO_AUTO_APPROVAL_THRESHOLD = "po_auto_approval_threshold";

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


    public boolean isMaintenanceMode() {
        return settingRepository.findBySettingKey(MAINTENANCE_MODE)
                .map(s -> Boolean.parseBoolean(s.getSettingValue()))
                .orElse(false);
    }

    public boolean isNotificationsEnabled() {
        return settingRepository.findBySettingKey(NOTIFICATIONS_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getSettingValue()))
                .orElse(true);
    }

    public int getMaxFileUploadSize() {
        return settingRepository.findBySettingKey(MAX_FILE_UPLOAD_SIZE_MB)
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(5);
    }

    public int getPoAutoApprovalThreshold() {
        return settingRepository.findBySettingKey(PO_AUTO_APPROVAL_THRESHOLD)
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(10);
    }

    @Transactional
    public void updateGeneralSettings(boolean maintenanceMode, boolean notificationsEnabled, int maxFileSize, int poThreshold) {
        upsert(MAINTENANCE_MODE, String.valueOf(maintenanceMode), "Sistem bakım modu (true/false)");
        upsert(NOTIFICATIONS_ENABLED, String.valueOf(notificationsEnabled), "Sistem içi bildirimler ve mailler aktif mi (true/false)");
        upsert(MAX_FILE_UPLOAD_SIZE_MB, String.valueOf(maxFileSize), "Maksimum dosya yükleme sınırı (MB)");
        upsert(PO_AUTO_APPROVAL_THRESHOLD, String.valueOf(poThreshold), "PO onayında otomatik işe dönüşme eşik puanı");
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