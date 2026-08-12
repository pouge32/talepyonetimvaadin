package com.example.base.ui.AdminScreen.AdminManagment;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.example.base.ui.Chat.InternalChatPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;

public class AdminDialogsHelper {

    private final RequestService requestService;
    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final NotificationService notificationService;
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;
    private final Runnable onRefreshGrid;

    public AdminDialogsHelper(RequestService requestService, RequestRepository requestRepository, WorkflowRepository workflowRepository,
                              UserRepository userRepository, SystemLogService systemLogService, NotificationService notificationService,
                              InternalCommentService internalCommentService, TeamChatBroadcaster teamChatBroadcaster, Runnable onRefreshGrid) {
        this.requestService = requestService;
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.notificationService = notificationService;
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;
        this.onRefreshGrid = onRefreshGrid;
    }

    public void openRequestDetailDialog(RequestEntity request) {
        Dialog detailDialog = new Dialog();
        detailDialog.setHeaderTitle("Talep Detayı #" + request.getRequestId());
        detailDialog.setWidth("650px");

        Tabs tabs = new Tabs();
        Tab detayTab = new Tab("Talep Bilgileri");
        Tab yazismaTab = new Tab("İç Yazışma (Takım)");
        tabs.add(detayTab, yazismaTab);

        VerticalLayout detayLayout = new VerticalLayout();
        detayLayout.setPadding(false);
        detayLayout.setSpacing(true);

        Span titleSpan = new Span("Başlık: " + request.getTitle());
        titleSpan.addClassName("admin-detail-title");

        TextArea descArea = new TextArea("Açıklama");
        descArea.setValue(request.getDescription() != null ? request.getDescription() : "Açıklama bulunmuyor.");
        descArea.setReadOnly(true);
        descArea.setWidthFull();
        descArea.setMinHeight("150px");

        detayLayout.add(titleSpan, descArea);

        try {
            WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
            if (workflow != null && workflow.getActualEffortHours() != null) {
                Span effortBadge = new Span("⏱️ Harcanan Efor: " + workflow.getActualEffortHours() + " Saat");
                effortBadge.getElement().getThemeList().add("badge success primary");
                effortBadge.addClassName("admin-effort-badge");
                detayLayout.add(effortBadge);
            }
        } catch (Exception ignored) {}

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByEmail(email).orElse(null);

        InternalChatPanel chatPanel = new InternalChatPanel(internalCommentService, teamChatBroadcaster, request.getRequestId(), currentUser);
        chatPanel.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            detayLayout.setVisible(event.getSelectedTab().equals(detayTab));
            chatPanel.setVisible(event.getSelectedTab().equals(yazismaTab));
        });

        detailDialog.add(tabs, detayLayout, chatPanel);
        Button closeBtn = new Button("Kapat", e -> detailDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        detailDialog.getFooter().add(closeBtn);
        detailDialog.open();
    }

    public void openPoDialog(RequestEntity request) {
        Dialog poDialog = new Dialog();
        poDialog.setHeaderTitle("Önceliklendirme ve Atama");

        ComboBox<String> impact = new ComboBox<>("Etki");
        impact.setItems("1 - Çok Düşük", "2 - Düşük", "3 - Orta", "4 - Yüksek", "5 - Kritik");
        ComboBox<String> urgency = new ComboBox<>("Aciliyet");
        urgency.setItems("1 - Çok Düşük", "2 - Düşük", "3 - Orta", "4 - Yüksek", "5 - Çok Acil");
        ComboBox<String> effort = new ComboBox<>("Efor");
        effort.setItems("1 - Çok Kısa", "2 - Kısa", "3 - Orta", "4 - Uzun", "5 - Çok Uzun");
        
        Checkbox securityOverride = new Checkbox("Güvenlik İhlali (Öncelikleri Geçersiz Kıl)");
        securityOverride.getStyle().set("margin-top", "15px").set("font-weight", "bold").set("color", "var(--lumo-error-text-color)");
        
        FormLayout fl = new FormLayout(impact, urgency, effort);
        fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button scoreBtn = new Button("Önceliklendir", e -> {
            boolean secOverride = securityOverride.getValue();
            if (!secOverride && (urgency.getValue() == null || impact.getValue() == null || effort.getValue() == null)) {
                Notification.show("Lütfen tüm alanları seçiniz.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                int urg = urgency.getValue() != null ? Character.getNumericValue(urgency.getValue().charAt(0)) : 5;
                int imp = impact.getValue() != null ? Character.getNumericValue(impact.getValue().charAt(0)) : 5;
                int eff = effort.getValue() != null ? Character.getNumericValue(effort.getValue().charAt(0)) : 1;
                requestService.prioritizeRequest(request.getRequestId(), urg, imp, eff, secOverride);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebini ONAYLANDI yaptı.");
                Notification.show("Başarıyla önceliklendirildi.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                poDialog.close();
                onRefreshGrid.run();
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage(), 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        scoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button toWorkflowBtn = new Button("Yazılım Görevine Dönüştür", e -> {
            try {
                requestService.goreveDonustur(request);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebini YAZILIM GÖREVİNE dönüştürdü.");
                Notification.show("Yazılım görevine dönüştürüldü.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                poDialog.close();
                onRefreshGrid.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        toWorkflowBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        
        poDialog.add(fl, securityOverride);
        poDialog.getFooter().add(scoreBtn, toWorkflowBtn, new Button("İptal", e -> poDialog.close()));
        poDialog.open();
    }

    public void openCloseDialog(RequestEntity request) {
        Dialog closeDialog = new Dialog();
        closeDialog.setHeaderTitle("Talebi Kapat / Reddet");
        TextArea closeReason = new TextArea("Kapatma / Ret Gerekçesi");
        closeReason.setWidthFull();

        Button confirmBtn = new Button("Kapat", e -> {
            request.setStatus("KAPATILDI");
            requestRepository.save(request);
            String admin = SecurityContextHolder.getContext().getAuthentication().getName();
            systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebini kapattı. Gerekçe: " + closeReason.getValue());
            Notification.show("Talep başarıyla kapatıldı.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            closeDialog.close();
            onRefreshGrid.run();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        
        closeDialog.add(closeReason);
        closeDialog.getFooter().add(confirmBtn, new Button("İptal", e -> closeDialog.close()));
        closeDialog.open();
    }

    public void openStatusDialog(RequestEntity request) {
        Dialog statusDialog = new Dialog();
        statusDialog.setHeaderTitle("Durum Güncelle");
        ComboBox<String> statusCombo = new ComboBox<>("Yeni Durum");
        statusCombo.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        statusCombo.setValue(request.getStatus());
        statusCombo.setWidthFull();

        Button saveBtn = new Button("Kaydet", e -> {
            if (statusCombo.getValue() != null) {
                request.setStatus(statusCombo.getValue());
                requestRepository.save(request);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " durumunu '" + statusCombo.getValue() + "' yaptı.");
                Notification.show("Durum güncellendi.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                statusDialog.close();
                onRefreshGrid.run();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        statusDialog.add(statusCombo);
        statusDialog.getFooter().add(saveBtn, new Button("İptal", e -> statusDialog.close()));
        statusDialog.open();
    }

    public void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle("Talep Geçmişi #" + request.getRequestId());
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());
            if (logs == null || logs.isEmpty()) {
                layout.add(new Span("Geçmiş bulunamadı."));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.addClassName("admin-history-step");
                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "Bilinmeyen Tarih";
                    Span dateSpan = new Span(dateStr);
                    dateSpan.addClassName("admin-history-date");
                    Span actionSpan = new Span(log.getAction());
                    actionSpan.addClassName("admin-history-action");
                    stepItem.add(dateSpan, actionSpan);
                    layout.add(stepItem);
                }
            }
        } catch (Exception e) {
            layout.add(new Span("Hata: " + e.getMessage()));
        }

        historyDialog.add(layout);
        historyDialog.getFooter().add(new Button("Kapat", e -> historyDialog.close()));
        historyDialog.open();
    }

    public void openScreenshotDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Ekran Görüntüsü #" + request.getRequestId());
        StreamResource res = new StreamResource("img.png", () -> new ByteArrayInputStream(request.getScreenshotData()));
        Image img = new Image(res, "Ekran Görüntüsü");
        img.setWidthFull();
        dialog.add(img);
        dialog.getFooter().add(new Button("Kapat", ev -> dialog.close()));
        dialog.open();
    }

    public void openRatingEditDialog(RequestEntity request) {
        Dialog ratingDialog = new Dialog();
        ratingDialog.setHeaderTitle("Puan/Yorum Düzenle");
        ratingDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        RadioButtonGroup<Integer> scoreGroup = new RadioButtonGroup<>();
        scoreGroup.setLabel("Puan");
        scoreGroup.setItems(1, 2, 3, 4, 5);
        scoreGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        if (request.getSatisfactionScore() != null) scoreGroup.setValue(request.getSatisfactionScore());
        
        TextArea commentArea = new TextArea("Yorum");
        commentArea.setWidthFull();
        if (request.getSatisfactionComment() != null) commentArea.setValue(request.getSatisfactionComment());

        layout.add(scoreGroup, commentArea);
        ratingDialog.add(layout);

        Button submitBtn = new Button("Kaydet", event -> {
            if (scoreGroup.getValue() == null) {
                Notification.show("Lütfen puan giriniz.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            request.setSatisfactionScore(scoreGroup.getValue());
            request.setSatisfactionComment(commentArea.getValue());
            requestRepository.save(request);

            String admin = SecurityContextHolder.getContext().getAuthentication().getName();
            systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebinin puanını " + scoreGroup.getValue() + " yaptı.");
            Notification.show("Puan başarıyla güncellendi.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            ratingDialog.close();
            onRefreshGrid.run();
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        ratingDialog.getFooter().add(submitBtn, new Button("İptal", e -> ratingDialog.close()));
        ratingDialog.open();
    }
}