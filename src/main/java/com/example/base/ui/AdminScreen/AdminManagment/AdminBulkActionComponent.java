package com.example.base.ui.AdminScreen.AdminManagment;

import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.service.NotificationService;
import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class AdminBulkActionComponent extends HorizontalLayout {

    private final Grid<RequestEntity> grid;
    private final RequestRepository requestRepository;
    private final SystemLogService systemLogService;
    private final NotificationService notificationService;
    private final Runnable onRefreshGrid;

    private final Span selectedCountLabel = new Span();
    private final Dialog bulkCloseDialog = new Dialog();
    private final TextArea bulkCloseReason = new TextArea();
    private final Dialog bulkStatusDialog = new Dialog();
    private final ComboBox<String> bulkStatusCombo = new ComboBox<>();

    public AdminBulkActionComponent(Grid<RequestEntity> grid, RequestRepository requestRepository, 
                                    SystemLogService systemLogService, NotificationService notificationService,
                                    Runnable onRefreshGrid) {
        this.grid = grid;
        this.requestRepository = requestRepository;
        this.systemLogService = systemLogService;
        this.notificationService = notificationService;
        this.onRefreshGrid = onRefreshGrid;

        setVisible(false);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassName("admin-bulk-action-bar");

        selectedCountLabel.addClassName("admin-selected-count-label"); 

        configureBulkCloseDialog();
        configureBulkStatusDialog();

        Button bulkStatusBtn = new Button(getTranslation("admin.management.bulk.changeStatus"), VaadinIcon.EXCHANGE.create());
        bulkStatusBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        bulkStatusBtn.addClickListener(e -> bulkStatusDialog.open());

        Button bulkCloseBtn = new Button(getTranslation("admin.management.bulk.closeReject"), VaadinIcon.CLOSE_CIRCLE.create());
        bulkCloseBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        bulkCloseBtn.addClickListener(e -> bulkCloseDialog.open());

        add(selectedCountLabel, bulkStatusBtn, bulkCloseBtn);
    }

    public void updateSelectionCount(int size) {
        if (size > 0) {
            setVisible(true);
            selectedCountLabel.setText(getTranslation("admin.management.bulkSelectedCount", size));
        } else {
            setVisible(false);
        }
    }

    private void configureBulkCloseDialog() {
        bulkCloseDialog.setHeaderTitle(getTranslation("admin.management.bulkClose.title"));
        bulkCloseReason.setLabel(getTranslation("admin.management.bulkClose.reasonLabel"));
        bulkCloseReason.setWidthFull();

        Button confirmBtn = new Button(getTranslation("admin.management.bulkClose.confirm"), e -> {
            Set<RequestEntity> selectedItems = grid.getSelectedItems();
            String admin = SecurityContextHolder.getContext().getAuthentication().getName();
            String reason = bulkCloseReason.getValue();
            for (RequestEntity req : selectedItems) {
                req.setStatus("KAPATILDI");
                requestRepository.save(req);
                systemLogService.log("Admin (" + admin + "), ID: " + req.getRequestId() + " talebini TOPLU İŞLEM ile kapattı. Gerekçe: " + reason);
                if (req.getCustomer() != null) {
                    notificationService.notifyUser(req.getCustomer().getUserId(), getTranslation("admin.management.notif.closedTitle"), getTranslation("admin.management.notif.closedContent", reason));
                }
            }
            Notification.show(getTranslation("admin.management.notification.bulkClosedSuccess", selectedItems.size()), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            bulkCloseDialog.close();
            bulkCloseReason.clear();
            grid.deselectAll();
            if (onRefreshGrid != null) onRefreshGrid.run();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> bulkCloseDialog.close());
        bulkCloseDialog.add(bulkCloseReason);
        bulkCloseDialog.getFooter().add(confirmBtn, cancelBtn);
    }

    private void configureBulkStatusDialog() {
        bulkStatusDialog.setHeaderTitle(getTranslation("admin.management.bulkStatus.title"));
        bulkStatusCombo.setLabel(getTranslation("admin.management.bulkStatus.label"));
        bulkStatusCombo.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        bulkStatusCombo.setWidthFull();

        Button saveBtn = new Button(getTranslation("admin.management.bulkStatus.confirm"), e -> {
            if (bulkStatusCombo.getValue() != null) {
                Set<RequestEntity> selectedItems = grid.getSelectedItems();
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                String newStatus = bulkStatusCombo.getValue();
                for (RequestEntity req : selectedItems) {
                    req.setStatus(newStatus);
                    requestRepository.save(req);
                    systemLogService.log("Admin (" + admin + "), ID: " + req.getRequestId() + " talebinin durumunu TOPLU İŞLEM ile '" + newStatus + "' yaptı.");
                }
                Notification.show(getTranslation("admin.management.notification.bulkStatusSuccess", selectedItems.size()), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                bulkStatusDialog.close();
                bulkStatusCombo.clear();
                grid.deselectAll();
                if (onRefreshGrid != null) onRefreshGrid.run();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> bulkStatusDialog.close());
        bulkStatusDialog.add(bulkStatusCombo);
        bulkStatusDialog.getFooter().add(saveBtn, cancelBtn);
    }
}