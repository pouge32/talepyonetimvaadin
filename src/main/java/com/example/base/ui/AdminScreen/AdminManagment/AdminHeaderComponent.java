package com.example.base.ui.AdminScreen.AdminManagment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.service.DemoDataService;
import com.example.base.service.ExcelExportService;
import com.example.base.service.PdfExportService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

public class AdminHeaderComponent extends Div {

    private final DemoDataService demoDataService;
    private final RequestService requestService;
    private final RequestRepository requestRepository;
    private final SystemLogService systemLogService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final Runnable onRefreshGrid;

    public AdminHeaderComponent(DemoDataService demoDataService, RequestService requestService,
                                RequestRepository requestRepository, SystemLogService systemLogService,
                                ExcelExportService excelExportService, PdfExportService pdfExportService,
                                Runnable onRefreshGrid) {
        this.demoDataService = demoDataService;
        this.requestService = requestService;
        this.requestRepository = requestRepository;
        this.systemLogService = systemLogService;
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
        this.onRefreshGrid = onRefreshGrid;

        setWidthFull();
        addClassName("admin-header");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        titleRow.addClassName("admin-title-row");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        H2 title = new H2(getTranslation("admin.management.headerTitle"));
        title.addClassName("admin-title");

        Paragraph subtitle = new Paragraph(getTranslation("admin.management.headerSubtitle"));
        subtitle.addClassName("admin-subtitle");

        textLayout.add(title, subtitle);

        titleRow.add(textLayout, buildActionButtons());
        add(titleRow);
    }

    private HorizontalLayout buildActionButtons() {
        Button resetDemoBtn = new Button(getTranslation("admin.management.btn.resetDemo"), VaadinIcon.TRASH.create());
        resetDemoBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Dialog confirmResetDialog = new Dialog();
        confirmResetDialog.setHeaderTitle(getTranslation("admin.management.dialog.resetTitle"));
        confirmResetDialog.add(new Paragraph(getTranslation("admin.management.dialog.resetText")));
        
        Button confirmDemoBtn = new Button(getTranslation("admin.management.btn.yesReset"), e -> executeDemoReset(confirmResetDialog));
        confirmDemoBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        
        Button cancelDemoBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> confirmResetDialog.close());
        confirmResetDialog.getFooter().add(confirmDemoBtn, cancelDemoBtn);
        resetDemoBtn.addClickListener(e -> confirmResetDialog.open());

        Button exportBtn = new Button(getTranslation("admin.management.btn.excel"), VaadinIcon.FILE_TABLE.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        StreamResource resource = new StreamResource("Talep_Raporu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx", 
            () -> excelExportService.exportRequestsToExcel(requestRepository.findAll()));
        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(exportBtn);

        Button pdfExportBtn = new Button(getTranslation("admin.management.btn.pdf"), VaadinIcon.FILE_TEXT_O.create());
        pdfExportBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        StreamResource pdfResource = new StreamResource("Talep_Raporu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf", 
            () -> pdfExportService.exportRequestsToPdf(requestRepository.findAll()));
        Anchor pdfDownloadLink = new Anchor(pdfResource, "");
        pdfDownloadLink.getElement().setAttribute("download", true);
        pdfDownloadLink.add(pdfExportBtn);

        HorizontalLayout actionButtons = new HorizontalLayout(resetDemoBtn, downloadLink, pdfDownloadLink);
        actionButtons.setSpacing(true);
        actionButtons.setAlignItems(FlexComponent.Alignment.CENTER);
        return actionButtons;
    }

    private void executeDemoReset(Dialog dialog) {
        demoDataService.resetSystemForDemo();
        try {
            int count = 0;
            for (RequestEntity req : requestRepository.findAll()) {
                if (count == 0) requestService.prioritizeRequest(req.getRequestId(), 5, 5, 1, true); 
                else if (count == 1) requestService.prioritizeRequest(req.getRequestId(), 4, 5, 1, false); 
                else if (count == 2) requestService.prioritizeRequest(req.getRequestId(), 3, 4, 1, false); 
                else if (count == 3) requestService.prioritizeRequest(req.getRequestId(), 2, 3, 1, false); 
                else if (count == 4) requestService.prioritizeRequest(req.getRequestId(), 2, 2, 2, false); 
                if (++count >= 5) break;
            }
        } catch (Exception ex) {}

        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        systemLogService.log("Admin (" + admin + ") sistemi DEMO modunda sıfırladı ve örnek öncelikler atadı.");
        Notification.show(getTranslation("admin.management.notification.resetSuccess"), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        dialog.close();
        if (onRefreshGrid != null) onRefreshGrid.run();
    }
}