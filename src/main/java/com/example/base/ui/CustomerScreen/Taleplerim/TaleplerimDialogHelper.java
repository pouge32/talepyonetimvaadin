package com.example.base.ui.CustomerScreen.Taleplerim;

import java.io.ByteArrayInputStream;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.RequestService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;

public class TaleplerimDialogHelper {

    private final Component context;
    private final RequestService requestService;
    private final WorkflowRepository workflowRepository;
    private final Runnable onRefreshGrid;

    public TaleplerimDialogHelper(Component context, RequestService requestService, 
                                  WorkflowRepository workflowRepository, Runnable onRefreshGrid) {
        this.context = context;
        this.requestService = requestService;
        this.workflowRepository = workflowRepository;
        this.onRefreshGrid = onRefreshGrid;
    }

    public void openRequestDetailDialog(RequestEntity request) {
        Dialog detailDialog = new Dialog();
        detailDialog.setHeaderTitle("Talep Detayı #" + request.getRequestId());
        detailDialog.setWidth("500px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        Span titleSpan = new Span("Başlık: " + request.getTitle());
        titleSpan.addClassName("taleplerim-detail-title");

        TextArea descArea = new TextArea("Açıklama");
        descArea.setValue(request.getDescription() != null ? request.getDescription() : "Açıklama bulunmuyor.");
        descArea.setReadOnly(true);
        descArea.setWidthFull();
        descArea.setMinHeight("150px");

        layout.add(titleSpan, descArea);

        try {
            WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
            if (workflow != null && workflow.getActualEffortHours() != null) {
                Span effortBadge = new Span("⏱️ Harcanan Efor: " + workflow.getActualEffortHours() + " Saat");
                effortBadge.getElement().getThemeList().add("badge success primary");
                effortBadge.addClassName("taleplerim-effort-badge");
                layout.add(effortBadge);
            }
        } catch (Exception ignored) {}

        detailDialog.add(layout);

        Button closeBtn = new Button("Kapat", e -> detailDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        detailDialog.getFooter().add(closeBtn);

        detailDialog.open();
    }

    public void openReopenDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(context.getTranslation("requests.dialog.reopenTitle") + " (#" + request.getRequestId() + ")");
        dialog.setWidth("500px");

        TextArea reasonArea = new TextArea(context.getTranslation("requests.dialog.reasonLabel"));
        reasonArea.setWidthFull();
        reasonArea.setPlaceholder(context.getTranslation("requests.dialog.reasonPlaceholder"));
        reasonArea.setRequired(true);

        Button confirmBtn = new Button(context.getTranslation("requests.btn.reopenConfirm"), e -> {
            if (reasonArea.isEmpty()) {
                Notification.show(context.getTranslation("requests.notification.reasonRequired"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                requestService.reopenRequest(request.getRequestId(), reasonArea.getValue());
                Notification.show(context.getTranslation("requests.notification.reopened"), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                if (onRefreshGrid != null) onRefreshGrid.run();
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button(context.getTranslation("requests.btn.cancel"), e -> dialog.close());

        dialog.add(new Paragraph(context.getTranslation("requests.dialog.reopenDesc")), reasonArea);
        dialog.getFooter().add(confirmBtn, cancelBtn);
        dialog.open();
    }

    public void openRatingDialog(RequestEntity request) {
        Dialog ratingDialog = new Dialog();
        ratingDialog.setHeaderTitle(context.getTranslation("requests.dialog.ratingTitle"));
        ratingDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();

        RadioButtonGroup<Integer> scoreGroup = new RadioButtonGroup<>();
        scoreGroup.setLabel(context.getTranslation("requests.dialog.scoreLabel"));
        scoreGroup.setItems(1, 2, 3, 4, 5);
        scoreGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        
        TextArea commentArea = new TextArea(context.getTranslation("requests.dialog.commentLabel"));
        commentArea.setPlaceholder(context.getTranslation("requests.dialog.commentPlaceholder"));
        commentArea.setWidthFull();

        layout.add(scoreGroup, commentArea);
        ratingDialog.add(layout);

        Button submitBtn = new Button(context.getTranslation("requests.btn.submitRating"), event -> {
            if (scoreGroup.getValue() == null) {
                Notification.show(context.getTranslation("requests.notification.scoreRequired"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            requestService.rateRequest(request.getRequestId(), scoreGroup.getValue(), commentArea.getValue());
            Notification.show(context.getTranslation("requests.notification.rated"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            ratingDialog.close();
            if (onRefreshGrid != null) onRefreshGrid.run();
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button(context.getTranslation("requests.btn.close"), e -> ratingDialog.close());

        ratingDialog.getFooter().add(submitBtn, cancelBtn);
        ratingDialog.open();
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
        image.addClassName("taleplerim-screenshot-image");

        Button closeBtn = new Button(context.getTranslation("requests.btn.close"), e -> dialog.close());

        dialog.add(image);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }
}