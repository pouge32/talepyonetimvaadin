package com.example.base.ui.PoScreen.TalepDegerlendirme;

import com.example.base.entity.PrioritizationEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.PrioritizationRepository;
import com.example.base.repository.RequestRepository;
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;

public class TalepActionDialogs {

    private final RequestService requestService;
    private final NotificationService notificationService;
    private final RequestRepository requestRepository;
    private final PrioritizationRepository prioritizationRepository;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;

    private final UserEntity currentUser;
    private final Runnable refreshCallback;
    private final Component context;

    public TalepActionDialogs(Component context, UserEntity currentUser, Runnable refreshCallback,
                              RequestService requestService, NotificationService notificationService,
                              RequestRepository requestRepository, PrioritizationRepository prioritizationRepository,
                              SystemLogService systemLogService, SettingsService settingsService) {
        this.context = context;
        this.currentUser = currentUser;
        this.refreshCallback = refreshCallback;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
    }

    public void openEvaluationDialog(RequestEntity request) {
        Dialog secim = new Dialog();
        secim.setHeaderTitle(context.getTranslation("po.eval.dialog.title"));
        secim.setWidth("450px");
        secim.setCloseOnOutsideClick(false);

        ComboBox<String> impact = new ComboBox<>(context.getTranslation("po.eval.impact"));
        impact.setItems(getComboItems("impact"));
        
        ComboBox<String> urgency = new ComboBox<>(context.getTranslation("po.eval.urgency"));
        urgency.setItems(getComboItems("urgency"));
        
        ComboBox<String> effort = new ComboBox<>(context.getTranslation("po.eval.effort"));
        effort.setItems(getComboItems("effort"));

        Checkbox securityOverride = new Checkbox(context.getTranslation("po.eval.securityOverride"));
        securityOverride.addClassName("po-eval-security-override");

        PrioritizationEntity p = prioritizationRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
        if (p != null) {
            if (p.getImpact() != null) impact.setValue(context.getTranslation("po.eval.impact." + p.getImpact()));
            if (p.getUrgency() != null) urgency.setValue(context.getTranslation("po.eval.urgency." + p.getUrgency()));
            if (p.getEffort() != null) effort.setValue(context.getTranslation("po.eval.effort." + p.getEffort()));
            securityOverride.setValue(p.getIsSecurityOverride() != null && p.getIsSecurityOverride() == 1);
        }

        FormLayout formLayout = new FormLayout(impact, urgency, effort);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button scoreButton = new Button(context.getTranslation("po.eval.btn.score"), e -> 
            evaluateRequest(request, urgency.getValue(), impact.getValue(), effort.getValue(), securityOverride.getValue(), secim)
        );

        Button aktar = new Button(context.getTranslation("po.eval.btn.transfer"), e -> 
            convertToWorkflow(request, urgency.getValue(), impact.getValue(), effort.getValue(), securityOverride.getValue(), secim)
        );
        aktar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        aktar.getElement().setProperty("title", context.getTranslation("po.eval.tooltip.transfer"));

        Button kapatBtn = new Button(context.getTranslation("po.eval.btn.closeReject"), e -> {
            secim.close();
            openCloseDialog(request);
        });
        kapatBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button iptal = new Button(context.getTranslation("po.eval.btn.cancel"), e -> secim.close());

        secim.getFooter().add(scoreButton, aktar, kapatBtn, iptal);
        secim.add(formLayout, securityOverride);
        secim.open();
    }

    public void openQaDialog(RequestEntity request) {
        Dialog qaDialog = new Dialog();
        qaDialog.setHeaderTitle("Yazılım Test & Onay");

        TextArea feedbackArea = new TextArea("Geri Bildirim / Not");
        feedbackArea.setWidthFull();
        feedbackArea.setPlaceholder("Test sonucuna göre notunuzu buraya yazın...");

        Button approveBtn = new Button("Onayla (Destek Ekibine Aktar)", e -> {
            request.setStatus("DESTEK_KONTROL");
            requestRepository.save(request);

            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() + " olan talebin yazılım testini ONAYLADI ve Destek ekibine aktardı. Not: " + feedbackArea.getValue());

            Notification.show("Talep test onayı alarak Destek Ekibine aktarıldı.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            qaDialog.close();
            refreshCallback.run();
        });
        approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        Button rejectBtn = new Button("Reddet (Yazılımcıya Geri Gönder)", e -> {
            request.setStatus("İş Akışına Dönüştü");
            requestRepository.save(request);

            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() + " olan talebin yazılım testini REDDETTİ. Gerekçe: " + feedbackArea.getValue());

            Notification.show("Talep reddedilerek düzeltilmesi için Yazılım Ekibine geri gönderildi.", 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            qaDialog.close();
            refreshCallback.run();
        });
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("İptal", e -> qaDialog.close());

        qaDialog.add(new Paragraph("Yazılım ekibi bu görevi tamamladığını belirtti. Lütfen test edip onay durumunu seçin."), feedbackArea);
        qaDialog.getFooter().add(approveBtn, rejectBtn, cancelBtn);
        qaDialog.open();
    }

    public void openCloseDialog(RequestEntity request) {
        Dialog closeDialog = new Dialog();
        closeDialog.setHeaderTitle(context.getTranslation("helpdesk.triage.dialog.closeTitle"));
        
        TextArea closeReason = new TextArea();
        closeReason.setLabel(context.getTranslation("helpdesk.triage.dialog.closeReasonLabel"));
        closeReason.setWidthFull();

        Button confirmCloseBtn = new Button(context.getTranslation("helpdesk.triage.dialog.closeConfirmBtn"), event -> {
            request.setStatus("KAPATILDI");
            requestRepository.save(request);

            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() + " olan talebi kapattı. Gerekçe: " + closeReason.getValue());

            if (request.getCustomer() != null) {
                notificationService.notifyUser(request.getCustomer().getUserId(), context.getTranslation("helpdesk.triage.notif.requestClosedTitle"), context.getTranslation("helpdesk.triage.notif.descPrefix") + ": " + closeReason.getValue());
            }
            Notification.show(context.getTranslation("helpdesk.triage.notif.closed"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            closeDialog.close();
            refreshCallback.run();
        });
        confirmCloseBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button(context.getTranslation("requests.btn.cancel"), e -> closeDialog.close());

        closeDialog.getFooter().add(confirmCloseBtn, cancelBtn);
        closeDialog.add(closeReason);
        closeDialog.open();
    }

    private void evaluateRequest(RequestEntity request, String urgencyVal, String impactVal, String effortVal, boolean secOverride, Dialog dialog) {
        if (!secOverride && (urgencyVal == null || impactVal == null || effortVal == null)) {
            Notification error = Notification.show(context.getTranslation("po.eval.error.selectFields"), 3000, Notification.Position.MIDDLE);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            int urgencyPuan = parseComboValue(urgencyVal);
            int impactPuan = parseComboValue(impactVal);
            int effortPuan = parseComboValue(effortVal);

            requestService.prioritizeRequest(request.getRequestId(), urgencyPuan, impactPuan, effortPuan, secOverride);

            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            String overrideText = secOverride ? " [KRİTİK GÜVENLİK/KESİNTİ]" : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() +  
                               " olan talebi önceliklendirdi." + overrideText + " (Aciliyet: " + urgencyPuan + 
                               ", Etki: " + impactPuan + ", Efor: " + effortPuan + ")");

            if (request.getCustomer() != null) {
                notificationService.notifyUser(request.getCustomer().getUserId(), context.getTranslation("po.eval.notif.prioritizedTitle"), "'" + request.getTitle() + "' " + context.getTranslation("po.eval.notif.prioritizedDesc"));
            }

            Notification.show(context.getTranslation("po.eval.notif.prioritizedSuccess"), 3000, Notification.Position.TOP_CENTER);
            dialog.close();
            refreshCallback.run();

        } catch (Exception e) {
            Notification.show("Hata: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
        }
    }

    private void convertToWorkflow(RequestEntity request, String urgencyVal, String impactVal, String effortVal, boolean secOverride, Dialog dialog) {
        if ("İş Akışına Dönüştü".equals(request.getStatus())) {
            Notification.show(context.getTranslation("po.eval.error.alreadyTransferred"), 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        
        try {
            int urgencyPuan = urgencyVal != null ? parseComboValue(urgencyVal) : 5;
            int impactPuan = impactVal != null ? parseComboValue(impactVal) : 5;
            int effortPuan = effortVal != null ? parseComboValue(effortVal) : 1;

            int score = requestService.calculateScore(urgencyPuan, impactPuan, effortPuan, secOverride);
            int threshold = settingsService.getPoAutoApprovalThreshold();

            if (score < threshold && !secOverride) {
                Notification.show("HATA: Bu talebin puanı (" + score + "), Admin tarafından belirlenen eşik değerinin (" + threshold + ") altında! Yazılım ekibine aktarılamaz.", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return; 
            }
            
            requestService.prioritizeRequest(request.getRequestId(), urgencyPuan, impactPuan, effortPuan, secOverride);
            requestService.goreveDonustur(request);

            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() + " olan talebi yazılım ekibine/göreve dönüştürdü.");

            if (request.getCustomer() != null) {
                notificationService.notifyUser(request.getCustomer().getUserId(), context.getTranslation("po.eval.notif.convertedTitle"), "'" + request.getTitle() + "' " + context.getTranslation("po.eval.notif.convertedDesc"));
            }

            Notification success = Notification.show(context.getTranslation("po.eval.notif.convertedSuccess"), 3000, Notification.Position.TOP_CENTER);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            dialog.close();
            refreshCallback.run();

        } catch (Exception e) {
            Notification error = Notification.show("Hata: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private int parseComboValue(String value) {
        if (value == null || value.isEmpty()) return 5;
        try {
            return Integer.parseInt(value.split(" ")[0]);
        } catch (Exception e) {
            return 5;
        }
    }

    private String[] getComboItems(String key) {
        return new String[]{
            context.getTranslation("po.eval." + key + ".1"),
            context.getTranslation("po.eval." + key + ".2"),
            context.getTranslation("po.eval." + key + ".3"),
            context.getTranslation("po.eval." + key + ".4"),
            context.getTranslation("po.eval." + key + ".5")
        };
    }
}