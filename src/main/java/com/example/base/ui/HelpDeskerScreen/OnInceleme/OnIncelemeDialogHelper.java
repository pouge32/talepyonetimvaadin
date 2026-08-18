package com.example.base.ui.HelpDeskerScreen.OnInceleme;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;

public class OnIncelemeDialogHelper {

    private final Component context;
    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final WorkflowRepository workflowRepository;
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;
    private final UserEntity currentUser;
    private final Runnable onRefreshGrid;

    public OnIncelemeDialogHelper(Component context, RequestRepository requestRepository, 
                                  NotificationService notificationService, SystemLogService systemLogService,
                                  WorkflowRepository workflowRepository, InternalCommentService internalCommentService,
                                  TeamChatBroadcaster teamChatBroadcaster, UserEntity currentUser, 
                                  Runnable onRefreshGrid) {
        this.context = context;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
        this.workflowRepository = workflowRepository;
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;
        this.currentUser = currentUser;
        this.onRefreshGrid = onRefreshGrid;
    }

    public void openCloseDialog(RequestEntity request) {
        Dialog closeDialog = new Dialog();
        closeDialog.setHeaderTitle(context.getTranslation("helpdesk.triage.dialog.closeTitle"));
        
        TextArea closeReason = new TextArea(context.getTranslation("helpdesk.triage.dialog.closeReasonLabel"));
        closeReason.setWidthFull();

        Button confirmCloseBtn = new Button(context.getTranslation("helpdesk.triage.dialog.closeConfirmBtn"), event -> {
            request.setStatus("KAPATILDI");
            requestRepository.save(request);

            String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
            systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi kapattı. Gerekçe: " + closeReason.getValue());

            if (request.getCustomer() != null) {
                notificationService.notifyUser(request.getCustomer().getUserId(), 
                        context.getTranslation("helpdesk.triage.notif.requestClosedTitle"), 
                        context.getTranslation("helpdesk.triage.notif.descPrefix") + ": " + closeReason.getValue());
            }
            Notification.show(context.getTranslation("helpdesk.triage.notif.closed"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            closeDialog.close();
            if (onRefreshGrid != null) onRefreshGrid.run();
        });
        confirmCloseBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        Button cancelBtn = new Button(context.getTranslation("requests.btn.cancel"), e -> closeDialog.close());

        closeDialog.getFooter().add(confirmCloseBtn, cancelBtn);
        closeDialog.add(closeReason);
        closeDialog.open();
    }

    public void openCustomerConfirmationDialog(RequestEntity request) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Müşteri Teyidi (QA Son Adım)");
        confirmDialog.setWidth("450px");

        TextArea noteArea = new TextArea("Müşteri Görüşme Notu");
        noteArea.setWidthFull();
        noteArea.setPlaceholder("Müşteri ile yapılan teyit görüşmesi detaylarını buraya yazın...");

        Button closeRequestBtn = new Button("Kapat (Sorun Çözüldü)", e -> {
            if (noteArea.getValue().trim().isEmpty()) {
                Notification.show("Lütfen görüşme notu giriniz.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            request.setStatus("KAPATILDI");
            requestRepository.save(request);

            String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
            systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi Müşteri Teyidi ile KAPATTI. Not: " + noteArea.getValue());

            if (request.getCustomer() != null) {
                notificationService.notifyUser(request.getCustomer().getUserId(), "Talep Çözüldü", 
                        "Talebiniz Destek Ekibi tarafından teyit edilerek başarıyla kapatılmıştır. Not: " + noteArea.getValue());
            }

            Notification.show("Talep müşteri teyidi ile başarıyla kapatıldı.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            confirmDialog.close();
            if (onRefreshGrid != null) onRefreshGrid.run();
        });
        closeRequestBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        Button returnToPoBtn = new Button("PO'ya Geri Gönder (Çözülmedi)", e -> {
            if (noteArea.getValue().trim().isEmpty()) {
                Notification.show("Lütfen geri gönderme gerekçesi giriniz.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            request.setStatus("INCELEMEDE"); 
            requestRepository.save(request);

            String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
            systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi Müşteri Teyidi aşamasında ÇÖZÜLEMEDİĞİ için PO'ya GERİ GÖNDERDİ. Gerekçe: " + noteArea.getValue());

            Notification.show("Talep incelenmesi için yeniden Ürün Yöneticisi'ne (PO) gönderildi.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            confirmDialog.close();
            if (onRefreshGrid != null) onRefreshGrid.run();
        });
        returnToPoBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("İptal", e -> confirmDialog.close());

        VerticalLayout dialogLayout = new VerticalLayout(new Paragraph("Yazılım süreci tamamlanan bu talep için lütfen müşteri ile iletişime geçip çözümün başarılı olup olmadığını teyit edin."), noteArea);
        dialogLayout.setPadding(false);

        confirmDialog.add(dialogLayout);
        confirmDialog.getFooter().add(closeRequestBtn, returnToPoBtn, cancelBtn);
        confirmDialog.open();
    }

    public void openRequestDetailDialog(RequestEntity request) {
        Dialog detailDialog = new Dialog();
        detailDialog.setHeaderTitle("Talep Detayı #" + request.getRequestId());
        detailDialog.setWidth("650px"); 
        detailDialog.setMaxHeight("85vh");

        Tabs tabs = new Tabs();
        Tab detayTab = new Tab("Talep Bilgileri");
        Tab yazismaTab = new Tab("İç Yazışma (Takım)");
        tabs.add(detayTab, yazismaTab);

        VerticalLayout detayLayout = new VerticalLayout();
        detayLayout.setPadding(false);
        detayLayout.setSpacing(true);

        Span titleSpan = new Span("Başlık: " + request.getTitle());
        titleSpan.getStyle().set("font-weight", "bold");

        TextArea descArea = new TextArea("Açıklama");
        descArea.setValue(request.getDescription() != null ? request.getDescription() : "Açıklama bulunmuyor.");
        descArea.setReadOnly(true);
        descArea.setWidthFull();
        descArea.setMinHeight("100px");

        detayLayout.add(titleSpan, descArea);

        try {
            WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
            if (workflow != null && workflow.getActualEffortHours() != null) {
                Span effortBadge = new Span("⏱️ Harcanan Efor: " + workflow.getActualEffortHours() + " Saat");
                effortBadge.getElement().getThemeList().add("badge success primary");
                effortBadge.getStyle().set("margin-top", "10px").set("font-size", "14px");
                detayLayout.add(effortBadge);
            }
        } catch (Exception ignored) {}

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());
            if (logs != null && !logs.isEmpty()) {
                detayLayout.add(new com.vaadin.flow.component.html.Hr());
                Span historyTitle = new Span("Talep Geçmişi");
                historyTitle.addClassName("on-inceleme-history-title");
                detayLayout.add(historyTitle);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.addClassName("on-inceleme-history-step");

                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "";
                    Span dateSpan = new Span(dateStr);
                    dateSpan.addClassName("on-inceleme-history-date");

                    Span actionSpan = new Span(log.getAction());
                    actionSpan.addClassName("on-inceleme-history-action");

                    stepItem.add(dateSpan, actionSpan);
                    detayLayout.add(stepItem);
                }
            }
        } catch (Exception ignored) {}
        
        com.example.base.ui.Chat.InternalChatPanel chatPanel = 
            new com.example.base.ui.Chat.InternalChatPanel(internalCommentService, teamChatBroadcaster, request.getRequestId(), currentUser);
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

    public void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle("Talep Geçmişi (#" + request.getRequestId() + ")");
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());

            if (logs == null || logs.isEmpty()) {
                layout.add(new Span("Bu talep için henüz bir geçmiş bulunmuyor."));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.addClassName("on-inceleme-history-step");

                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "-";
                    Span dateSpan = new Span(dateStr);
                    dateSpan.addClassName("on-inceleme-history-date");

                    Span actionSpan = new Span(log.getAction());
                    actionSpan.addClassName("on-inceleme-history-action");

                    stepItem.add(dateSpan, actionSpan);
                    layout.add(stepItem);
                }
            }
        } catch (Exception e) {
            layout.add(new Span("Geçmiş yüklenirken hata oluştu: " + e.getMessage()));
        }

        historyDialog.add(layout);
        Button closeBtn = new Button("Kapat", e -> historyDialog.close());
        historyDialog.getFooter().add(closeBtn);
        
        historyDialog.open();
    }

    public void openScreenshotDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(context.getTranslation("requests.dialog.screenshotTitle") + " #" + request.getRequestId());
        dialog.setWidth("640px");
        dialog.setCloseOnOutsideClick(true);

        String fileName = request.getScreenshotFileName() != null ? request.getScreenshotFileName() : "ekran-goruntusu.png";
        StreamResource resource = new StreamResource(fileName,
                () -> new ByteArrayInputStream(request.getScreenshotData()));

        Image image = new Image(resource, "Ekran görüntüsü");
        image.setWidthFull();
        image.addClassName("on-inceleme-screenshot-image");

        Button closeBtn = new Button(context.getTranslation("requests.btn.close"), e -> dialog.close());

        dialog.add(image);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }
}