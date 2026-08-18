package com.example.base.ui.ProgrammerScreen.ProgrammerTask;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.SubTaskEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.SubTaskRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

public class ProgrammerTaskDialogs {

    private final WorkflowRepository workflowRepository;
    private final RequestRepository requestRepository;
    private final SubTaskRepository subTaskRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;

    private final UserEntity currentUser;
    private final Runnable refreshGridCallback;
    private final Consumer<WorkflowEntity> refreshItemCallback;
    private final Component context;

    public ProgrammerTaskDialogs(Component context, UserEntity currentUser, Runnable refreshGridCallback, Consumer<WorkflowEntity> refreshItemCallback,
                                 WorkflowRepository workflowRepository, RequestRepository requestRepository, SubTaskRepository subTaskRepository,
                                 NotificationService notificationService, SystemLogService systemLogService, 
                                 InternalCommentService internalCommentService, TeamChatBroadcaster teamChatBroadcaster) {
        this.context = context;
        this.currentUser = currentUser;
        this.refreshGridCallback = refreshGridCallback;
        this.refreshItemCallback = refreshItemCallback;
        this.workflowRepository = workflowRepository;
        this.requestRepository = requestRepository;
        this.subTaskRepository = subTaskRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;
    }

    public void updateWorkflowStatus(WorkflowEntity workflow, String newStatus, ComboBox<String> statusCombo, String oldStatus) {
        if (newStatus == null || newStatus.equals(workflow.getWorkflowStatus())) {
            return;
        }

        if ("DONE".equals(newStatus)) {
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Görevi Tamamla ve Detay Gir");
            confirmDialog.setWidth("500px");
            
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.add(new Paragraph("Yazılım sürecini tamamlayıp PO onayına aktarmak üzeresiniz. Lütfen gerekli alanları doldurunuz."));

            IntegerField effortField = new IntegerField("Harcanan Toplam Efor (Saat)");
            effortField.setMin(1);
            effortField.setStepButtonsVisible(true);
            effortField.setRequiredIndicatorVisible(true);
            effortField.setWidthFull();

            TextField prLinkField = new TextField("PR Linki / Commit ID (Opsiyonel)");
            prLinkField.setPlaceholder("Örn: https://github.com/.../pull/12 veya 7a8f9c...");
            prLinkField.setWidthFull();
            prLinkField.setClearButtonVisible(true);

            TextArea noteField = new TextArea("Tamamlama Notu / Açıklama (Opsiyonel)");
            noteField.setPlaceholder("Karşılaşılan zorluklar veya PO'nun bilmesi gereken teknik notlar...");
            noteField.setWidthFull();

            layout.add(effortField, prLinkField, noteField);
            confirmDialog.add(layout);

            Button confirmBtn = new Button("Tamamla ve Gönder", event -> {
                if (effortField.getValue() == null || effortField.getValue() <= 0) {
                    Notification.show("Lütfen harcanan eforu saat cinsinden belirtiniz!", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                workflow.setActualEffortHours(effortField.getValue());
                executeStatusUpdate(workflow, newStatus, effortField.getValue(), noteField.getValue(), prLinkField.getValue());
                confirmDialog.close();
            });
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

            Button cancelBtn = new Button("İptal", event -> {
                statusCombo.setValue(oldStatus); 
                confirmDialog.close();
            });
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            confirmDialog.getFooter().add(confirmBtn, cancelBtn);
            confirmDialog.open();
        } else {
            executeStatusUpdate(workflow, newStatus, null, null, null);
        }
    }

    private void executeStatusUpdate(WorkflowEntity workflow, String newStatus, Integer effort, String note, String prLink) {
        try {
            workflow.setWorkflowStatus(newStatus);
            if (prLink != null && !prLink.trim().isEmpty()) {
                workflow.setPrLink(prLink.trim());
            }

            workflowRepository.save(workflow);
            RequestEntity request = workflow.getRequest();
            
            if ("DONE".equals(newStatus) && request != null) {
                request.setStatus("PO_KONTROL");
                requestRepository.save(request);
            }

            String devEmail = (currentUser != null) ? currentUser.getEmail() : "";
            String logMsg = context.getTranslation("programmer.log.updated") + " (" + devEmail + ") " + 
                            context.getTranslation("programmer.log.taskId") + ": " + workflow.getTaskId() + " " + 
                            context.getTranslation("programmer.log.status") + ": " + newStatus;
                            
            if ("DONE".equals(newStatus) && effort != null) {
                logMsg += " | Harcanan Efor: " + effort + " Saat";
                if (prLink != null && !prLink.trim().isEmpty()) logMsg += " | PR/Commit: " + prLink;
                if (note != null && !note.trim().isEmpty()) logMsg += " | Geliştirici Notu: " + note;
            }
            systemLogService.log(logMsg); 
            
            if (request != null && request.getCustomer() != null) {
                String notifMessage = "DONE".equals(newStatus) 
                        ? "Talebinizin yazılım süreci tamamlandı, kalite ve test kontrolü için Ürün Yöneticisi'ne (PO) aktarıldı." 
                        : context.getTranslation("programmer.notif.message") + " " + newStatus;
                notificationService.notifyUser(request.getCustomer().getUserId(), context.getTranslation("programmer.notif.title"), notifMessage);
            }

            Notification.show(context.getTranslation("programmer.notif.success"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            refreshGridCallback.run();

        } catch (Exception ex) {
            Notification.show("Hata: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    public void showDetails(WorkflowEntity workflow) {
        Dialog detailDialog = new Dialog(); 
        detailDialog.setHeaderTitle(context.getTranslation("programmer.dialog.title"));
        detailDialog.setWidth("650px"); 
        detailDialog.setMaxHeight("85vh"); 

        Tabs tabs = new Tabs();
        Tab detayTab = new Tab("Talep Bilgileri");
        Tab yazismaTab = new Tab("İç Yazışma (Takım)");
        tabs.add(detayTab, yazismaTab);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        if (workflow.getRequest() != null) {
            String rejectionReason = null;
            try {
                var logs = systemLogService.getLogsForRequest(workflow.getRequest().getRequestId());
                if (logs != null) {
                    for (var log : logs) {
                        String actionText = log.getAction();
                        if (actionText != null && (actionText.contains("REDDETTİ") || actionText.toLowerCase().contains("reddetti"))) {
                            int idx = actionText.indexOf("Gerekçe:");
                            if (idx != -1) rejectionReason = actionText.substring(idx + 8).trim();
                            else rejectionReason = actionText; 
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (rejectionReason != null) {
                Span rejectBadge = new Span("PO Tarafından Kalite Kontrolü Reddedildi");
                rejectBadge.getElement().getThemeList().add("badge error");
                rejectBadge.getStyle().set("margin-bottom", "10px").set("font-size", "14px");
                
                TextArea rejectArea = new TextArea("PO Ret Gerekçesi / İstenen Düzeltmeler");
                rejectArea.setValue(rejectionReason);
                rejectArea.setReadOnly(true);
                rejectArea.setWidthFull();
                rejectArea.addClassName("programmer-reject-area");
                
                content.add(rejectBadge, rejectArea);
            }

            Span title = new Span(context.getTranslation("programmer.dialog.titleLabel") + ": " + workflow.getRequest().getTitle());
            title.addClassName("programmer-detail-title");
            
            TextArea desc = new TextArea(context.getTranslation("programmer.dialog.descLabel"));
            desc.setValue(workflow.getRequest().getDescription() != null ? workflow.getRequest().getDescription() : "");
            desc.setReadOnly(true);
            desc.setWidthFull();
            desc.setMinHeight("100px");
            
            content.add(title, desc);

            content.add(new com.vaadin.flow.component.html.Hr());
            Span checklistTitle = new Span("Alt Görevler (Checklist)");
            checklistTitle.addClassName("programmer-section-title");
            content.add(checklistTitle);

            VerticalLayout tasksContainer = new VerticalLayout();
            tasksContainer.setPadding(false);
            tasksContainer.setSpacing(false);

            refreshChecklist(tasksContainer, workflow);

            HorizontalLayout addTaskLayout = new HorizontalLayout();
            TextField newTaskField = new TextField();
            newTaskField.setPlaceholder("Örn: Tabloların oluşturulması...");
            newTaskField.setWidthFull();
            Button addTaskBtn = new Button("Ekle", VaadinIcon.PLUS.create());
            addTaskBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            
            addTaskBtn.addClickListener(e -> {
                if (newTaskField.getValue() != null && !newTaskField.getValue().trim().isEmpty()) {
                    SubTaskEntity newTask = new SubTaskEntity();
                    newTask.setWorkflow(workflow);
                    newTask.setDescription(newTaskField.getValue().trim());
                    newTask.setCompleted(false);
                    subTaskRepository.save(newTask);
                    
                    newTaskField.clear();
                    refreshChecklist(tasksContainer, workflow); 
                    refreshItemCallback.accept(workflow);
                }
            });
            addTaskLayout.add(newTaskField, addTaskBtn);
            addTaskLayout.setWidthFull();
            addTaskLayout.setAlignItems(Alignment.BASELINE);

            content.add(tasksContainer, addTaskLayout);

            if (workflow.getPrLink() != null && !workflow.getPrLink().isEmpty()) {
                Span prBadge = new Span("🔗 PR / Commit: " + workflow.getPrLink());
                prBadge.getElement().getThemeList().add("badge contrast");
                prBadge.addClassName("programmer-detail-badge");
                content.add(prBadge);
            }

            if (workflow.getActualEffortHours() != null) {
                Span effortBadge = new Span("⏱️ Harcanan Efor: " + workflow.getActualEffortHours() + " Saat");
                effortBadge.getElement().getThemeList().add("badge success primary");
                effortBadge.addClassName("programmer-detail-badge");
                content.add(effortBadge);
            }

            try {
                var logs = systemLogService.getLogsForRequest(workflow.getRequest().getRequestId());
                if (logs != null && !logs.isEmpty()) {
                    content.add(new com.vaadin.flow.component.html.Hr());
                    Span historyTitle = new Span("Talep Geçmişi");
                    historyTitle.addClassName("programmer-section-title");
                    content.add(historyTitle);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    for (var log : logs) {
                        Div stepItem = new Div();
                        stepItem.addClassName("programmer-history-step");

                        String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "";
                        Span dateSpan = new Span(dateStr);
                        dateSpan.addClassName("programmer-history-date");

                        Span actionSpan = new Span(log.getAction());
                        actionSpan.addClassName("programmer-history-action");

                        stepItem.add(dateSpan, actionSpan);
                        content.add(stepItem);
                    }
                }
            } catch (Exception ignored) {}

        } else {
            content.add(new Span(context.getTranslation("programmer.dialog.noDetail")));
        }

        com.example.base.ui.Chat.InternalChatPanel chatPanel = 
            new com.example.base.ui.Chat.InternalChatPanel(internalCommentService, teamChatBroadcaster, workflow.getRequest().getRequestId(), currentUser);
        chatPanel.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            content.setVisible(event.getSelectedTab().equals(detayTab));
            chatPanel.setVisible(event.getSelectedTab().equals(yazismaTab));
        });

        detailDialog.add(tabs, content, chatPanel);

        Button closeBtn = new Button(context.getTranslation("requests.btn.close"), e -> detailDialog.close());
        detailDialog.getFooter().add(closeBtn);
        
        detailDialog.open();
    }

    private void refreshChecklist(VerticalLayout tasksContainer, WorkflowEntity workflow) {
        tasksContainer.removeAll();
        List<SubTaskEntity> tasks = subTaskRepository.findByWorkflow_TaskIdOrderByIdAsc(workflow.getTaskId());
        for (SubTaskEntity task : tasks) {
            Checkbox checkbox = new Checkbox(task.getDescription(), task.isCompleted());
            
            if (task.isCompleted()) {
                checkbox.addClassName("programmer-checkbox-completed");
            }
            
            checkbox.addValueChangeListener(e -> {
                task.setCompleted(e.getValue());
                subTaskRepository.save(task);
                
                String userEmail = (currentUser != null) ? currentUser.getEmail() : "";
                String statusText = e.getValue() ? "tamamlandı" : "bekliyor";
                systemLogService.log("Yazılımcı (" + userEmail + "), Alt Görev: '" + task.getDescription() + "' -> " + statusText);
                
                if (e.getValue()) {
                    checkbox.addClassName("programmer-checkbox-completed");
                } else {
                    checkbox.removeClassName("programmer-checkbox-completed");
                }
                
                refreshItemCallback.accept(workflow);
            });
            tasksContainer.add(checkbox);
        }
    }
}