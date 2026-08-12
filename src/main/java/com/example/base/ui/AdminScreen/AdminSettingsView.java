package com.example.base.ui.AdminScreen;

import java.util.List;

import com.example.base.entity.CategoryEntity;
import com.example.base.service.SettingsService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/ayarlar", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "GODPANEL"})
@CssImport("./styles/admin/admin-settings.css")
public class AdminSettingsView extends VerticalLayout implements HasDynamicTitle {

    private final SettingsService settingsService;

    private final Div categoryPanel = new Div();
    private final Div slaPanel = new Div();
    private final Div generalPanel = new Div();

    private final Grid<CategoryEntity> categoryGrid = new Grid<>(CategoryEntity.class, false);

    private final Tab tabCategories = new Tab();
    private final Tab tabSla = new Tab();
    private final Tab tabGeneral = new Tab();
    private final Tabs tabs = new Tabs(tabCategories, tabSla, tabGeneral);

    public AdminSettingsView(SettingsService settingsService) {
        this.settingsService = settingsService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("admin-settings-layout");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("admin-settings-container");

        H2 headerTitle = new H2(getTranslation("admin.settings.headerTitle", "Sistem Ayarları"));
        headerTitle.addClassName("admin-settings-header-title");
        
        Paragraph headerDesc = new Paragraph(getTranslation("admin.settings.headerDesc", "Kategorileri, SLA sürelerini ve genel sistem yapılandırmalarını buradan yönetebilirsiniz."));
        headerDesc.addClassName("admin-settings-header-desc");

        tabCategories.setLabel(getTranslation("admin.settings.tab.categories", "Kategoriler"));
        tabSla.setLabel(getTranslation("admin.settings.tab.sla", "SLA / Süre Ayarları"));
        tabGeneral.setLabel(getTranslation("admin.settings.tab.general", "Genel Ayarlar"));

        tabs.setWidthFull();
        tabs.addClassName("admin-settings-tabs");
        
        tabs.addSelectedChangeListener(e -> {
            categoryPanel.setVisible(tabs.getSelectedIndex() == 0);
            slaPanel.setVisible(tabs.getSelectedIndex() == 1);
            generalPanel.setVisible(tabs.getSelectedIndex() == 2);
        });

        categoryPanel.setWidthFull();
        slaPanel.setWidthFull();
        generalPanel.setWidthFull();

        buildCategoryPanel();
        buildSlaPanel();
        buildGeneralPanel();
        
        slaPanel.setVisible(false);
        generalPanel.setVisible(false);

        mainContainer.add(headerTitle, headerDesc, tabs, categoryPanel, slaPanel, generalPanel);
        add(mainContainer);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.settings.pageTitle", "Sistem Ayarları");
    }

    private void buildCategoryPanel() {
        categoryPanel.removeAll();

        H3 title = new H3(getTranslation("admin.settings.category.title", "Kategori Yönetimi"));
        title.addClassName("admin-settings-panel-title");

        TextField newCategoryField = new TextField();
        newCategoryField.setPlaceholder(getTranslation("admin.settings.category.placeholder", "Yeni Kategori Adı"));
        newCategoryField.setWidthFull();

        Button addBtn = new Button(getTranslation("admin.settings.btn.add", "Ekle"), e -> {
            String value = newCategoryField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show(getTranslation("admin.settings.category.errorEmpty", "Kategori adı boş olamaz"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                settingsService.addCategory(value.trim());
                newCategoryField.clear();
                refreshCategoryGrid();
                Notification.show(getTranslation("admin.settings.category.successAdded", "Kategori eklendi"), 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout addRow = new HorizontalLayout(newCategoryField, addBtn);
        addRow.setWidthFull();
        addRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        addRow.addClassName("admin-settings-add-row");

        categoryGrid.setWidthFull();
        categoryGrid.setHeight("400px");
        
        categoryGrid.addColumn(CategoryEntity::getName)
                .setHeader(getTranslation("admin.settings.grid.name", "Kategori Adı"))
                .setFlexGrow(1);
                
        categoryGrid.addComponentColumn(this::createActiveToggle)
                .setHeader(getTranslation("admin.settings.grid.status", "Durum"))
                .setAutoWidth(true).setFlexGrow(0);
                
        categoryGrid.addComponentColumn(this::createDeleteButton)
                .setHeader(getTranslation("admin.settings.grid.action", "İşlemler"))
                .setAutoWidth(true).setFlexGrow(0);

        refreshCategoryGrid();

        categoryPanel.add(title, addRow, categoryGrid);
    }

    private Component createActiveToggle(CategoryEntity category) {
        Button toggleBtn = new Button(category.isActive() ? getTranslation("admin.settings.btn.makePassive", "Pasif Yap") : getTranslation("admin.settings.btn.makeActive", "Aktif Yap"));
        toggleBtn.addThemeVariants(category.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        toggleBtn.addClickListener(e -> {
            settingsService.setCategoryActive(category.getCategoryId(), !category.isActive());
            refreshCategoryGrid();
        });
        return toggleBtn;
    }

    private Component createDeleteButton(CategoryEntity category) {
        Button deleteBtn = new Button(getTranslation("admin.settings.btn.deleteAll", "Sil"));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        deleteBtn.addClickListener(e -> {
            settingsService.deleteCategory(category.getCategoryId());
            refreshCategoryGrid();
            Notification.show(getTranslation("admin.settings.category.successDeleted", "Kategori silindi"), 2000, Notification.Position.TOP_CENTER);
        });
        return deleteBtn;
    }

    private void refreshCategoryGrid() {
        List<CategoryEntity> all = settingsService.getAllCategories(); 
        categoryGrid.setItems(all);
    }

    private void buildSlaPanel() {
        slaPanel.removeAll();

        H3 title = new H3(getTranslation("admin.settings.sla.title", "SLA (Servis Seviyesi) Ayarları"));
        title.addClassName("admin-settings-panel-title");
        
        Paragraph info = new Paragraph(getTranslation("admin.settings.sla.info", "Destek ve yazılım ekiplerinin taleplere müdahale sürelerini belirleyin."));
        info.addClassName("admin-settings-panel-info");

        NumberField limitField = new NumberField(getTranslation("admin.settings.sla.limitLabel", "SLA İhlal Sınırı (Saat)"));
        limitField.setValue((double) settingsService.getSlaLimitHours());
        limitField.setMin(1);
        limitField.setStep(1);
        limitField.setWidthFull();

        NumberField warningField = new NumberField(getTranslation("admin.settings.sla.warningLabel", "SLA Uyarı Yüzdesi (%)"));
        warningField.setValue(settingsService.getSlaWarningPercent() * 100);
        warningField.setMin(1);
        warningField.setMax(100);
        warningField.setStep(5);
        warningField.setHelperText(getTranslation("admin.settings.sla.warningHelper", "SLA süresi bu yüzdeye ulaştığında sarı uyarı verir."));
        warningField.setWidthFull();

        Button saveBtn = new Button(getTranslation("admin.settings.btn.saveSettings", "Ayarları Kaydet"), e -> {
            if (limitField.getValue() == null || warningField.getValue() == null) {
                Notification.show(getTranslation("admin.settings.sla.errorEmpty", "Alanlar boş bırakılamaz"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            long hours = limitField.getValue().longValue();
            double percent = warningField.getValue() / 100.0;
            settingsService.updateSlaSettings(hours, percent);
            Notification.show(getTranslation("admin.settings.sla.successSaved", "SLA ayarları başarıyla kaydedildi."), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClassName("admin-settings-save-btn");

        VerticalLayout form = new VerticalLayout(limitField, warningField, saveBtn);
        form.setPadding(false);
        form.setSpacing(false);
        form.setMaxWidth("400px");

        slaPanel.add(title, info, form);
    }

    private void buildGeneralPanel() {
        generalPanel.removeAll();

        H3 title = new H3("Genel Sistem Yapılandırması");
        title.addClassName("admin-settings-panel-title");
        
        Paragraph info = new Paragraph("Sistem genelindeki kısıtlamaları ve iş akışı parametrelerini buradan yönetebilirsiniz.");
        info.addClassName("admin-settings-panel-info-spaced");

        Checkbox maintenanceToggle = new Checkbox("Bakım Modunu Aktifleştir");
        maintenanceToggle.setValue(settingsService.isMaintenanceMode());
        maintenanceToggle.setHelperText("Aktif edildiğinde sadece Adminler giriş yapabilir, yeni talep açılamaz.");

        Checkbox notificationToggle = new Checkbox("Sistem İçi Bildirimleri ve E-postaları Aç");
        notificationToggle.setValue(settingsService.isNotificationsEnabled());

        NumberField maxFileSizeField = new NumberField("Maksimum Dosya Yükleme Boyutu (MB)");
        maxFileSizeField.setValue((double) settingsService.getMaxFileUploadSize());
        maxFileSizeField.setMin(1);
        maxFileSizeField.setMax(50);
        maxFileSizeField.setWidthFull();

        NumberField poThresholdField = new NumberField("PO Otomatik Görev Aktarım Puanı (Eşik)");
        poThresholdField.setValue((double) settingsService.getPoAutoApprovalThreshold());
        poThresholdField.setHelperText("Aciliyet ve etki puanlarının toplamı bu değeri geçerse talep otomatik yüksek öncelikli olur.");
        poThresholdField.setWidthFull();

        Button saveGeneralBtn = new Button(getTranslation("admin.settings.btn.saveSettings", "Ayarları Kaydet"), e -> {
            
            settingsService.updateGeneralSettings(
                maintenanceToggle.getValue(),
                notificationToggle.getValue(),
                maxFileSizeField.getValue().intValue(),
                poThresholdField.getValue().intValue()
            );

            Notification.show("Genel ayarlar başarıyla kaydedildi.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveGeneralBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveGeneralBtn.addClassName("admin-settings-save-btn");

        VerticalLayout form = new VerticalLayout(maintenanceToggle, notificationToggle, maxFileSizeField, poThresholdField, saveGeneralBtn);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("450px");

        generalPanel.add(title, info, form);
    }
}