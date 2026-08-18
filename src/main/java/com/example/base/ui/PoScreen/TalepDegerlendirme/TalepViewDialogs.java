package com.example.base.ui.PoScreen.TalepDegerlendirme;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.InternalCommentService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

public class TalepViewDialogs {

    private final SystemLogService systemLogService;
    private final WorkflowRepository workflowRepository;
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;
    private final UserEntity currentUser;
    private final Component context;

    public TalepViewDialogs(Component context, UserEntity currentUser, 
                            SystemLogService systemLogService, WorkflowRepository workflowRepository, 
                            InternalCommentService internalCommentService, TeamChatBroadcaster teamChatBroadcaster) {
        this.context = context;
        this.currentUser = currentUser;
        this.systemLogService = systemLogService;
        this.workflowRepository = workflowRepository;
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;
    }

    public void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle(context.getTranslation("po.eval.history.title") + " (#" + request.getRequestId() + ")");
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());

            if (logs == null || logs.isEmpty()) {
                layout.add(new Span(context.getTranslation("po.eval.history.empty")));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.addClassName("po-eval-history-step");

                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : context.getTranslation("po.eval.history.unknownDate");
                    Span dateSpan = new Span(dateStr);
                    dateSpan.addClassName("po-eval-history-date");

                    Span actionSpan = new Span(log.getAction());
                    actionSpan.addClassName("po-eval-history-action");

                    stepItem.add(dateSpan, actionSpan);
                    layout.add(stepItem);
                }
            }
        } catch (Exception e) {
            layout.add(new Span(context.getTranslation("po.eval.history.error") + ": " + e.getMessage()));
        }

        historyDialog.add(layout);
        Button closeBtn = new Button(context.getTranslation("requests.btn.close"), e -> historyDialog.close());
        historyDialog.getFooter().add(closeBtn);
        historyDialog.open();
    }

    public void openScreenshotDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(context.getTranslation("requests.dialog.screenshotTitle") + " #" + request.getRequestId());
        dialog.setWidth("640px");
        dialog.setCloseOnOutsideClick(true);

        String fileName = request.getScreenshotFileName() != null ? request.getScreenshotFileName() : "ekran-goruntusu.png";
        StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(request.getScreenshotData()));

        Image image = new Image(resource, "Ekran görüntüsü");
        image.setWidthFull();
        image.addClassName("po-eval-screenshot");

        Button closeBtn = new Button(context.getTranslation("requests.btn.close"), e -> dialog.close());

        dialog.add(image);
        dialog.getFooter().add(closeBtn);
        dialog.open();
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
        titleSpan.addClassName("po-eval-title-span");

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
                effortBadge.addClassName("po-eval-effort-badge");
                detayLayout.add(effortBadge);
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
}