package com.example.base.ui.AdminScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatService;
import com.example.base.service.DemoDataService;
import com.example.base.service.ExcelExportService;
import com.example.base.service.PdfExportService;
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Class.TalepChat;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin-paneli", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminManagmentView extends VerticalLayout implements HasDynamicTitle {

    private final RequestService requestService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final SystemLogService systemLogService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final DemoDataService demoDataService;

    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    private GridListDataView<RequestEntity> dataView;
    private final RequestFilter requestFilter = new RequestFilter();

    private final Dialog poDialog = new Dialog();
    private final ComboBox<String> urgency = new ComboBox<>();
    private final ComboBox<String> impact = new ComboBox<>();
    private final ComboBox<String> effort = new ComboBox<>();
    private final Checkbox securityOverride = new Checkbox();
    private RequestEntity selectedRequestForPo;

    private final Dialog closeDialog = new Dialog();
    private final TextArea closeReason = new TextArea();
    private RequestEntity selectedRequestForClose;

    private final Dialog statusDialog = new Dialog();
    private final ComboBox<String> statusCombo = new ComboBox<>();
    private RequestEntity selectedRequestForStatus;

    private final HorizontalLayout bulkActionBar = new HorizontalLayout();
    private final Span selectedCountLabel = new Span();
    
    private final Dialog bulkCloseDialog = new Dialog();
    private final TextArea bulkCloseReason = new TextArea();
    
    private final Dialog bulkStatusDialog = new Dialog();
    private final ComboBox<String> bulkStatusCombo = new ComboBox<>();

    public AdminManagmentView(RequestService requestService, NotificationService notificationService,
                              ChatService chatService, UserRepository userRepository,
                              RequestRepository requestRepository, SystemLogService systemLogService,
                              ExcelExportService excelExportService, PdfExportService pdfExportService,
                              DemoDataService demoDataService) { 
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.systemLogService = systemLogService;
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
        this.demoDataService = demoDataService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "20px") 
                .set("max-width", "100%") 
                .set("height", "calc(100vh - 100px)")
                .set("display", "flex")
                .set("flex-direction", "column");

        grid.setWidthFull();
        grid.getStyle().set("flex-grow", "1").set("border-radius", "12px");
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES); 

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addSelectionListener(event -> {
            int size = event.getAllSelectedItems().size();
            if (size > 0) {
                bulkActionBar.setVisible(true);
                selectedCountLabel.setText(getTranslation("admin.management.bulkSelectedCount", size));
            } else {
                bulkActionBar.setVisible(false);
            }
        });

        configureBulkActionBar();
        configureBulkCloseDialog();
        configureBulkStatusDialog();

        configureGrid();
        configurePoDialog();
        configureCloseDialog();
        configureStatusDialog();

        mainContainer.add(buildHeader(), bulkActionBar, grid);
        add(mainContainer);

        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.management.pageTitle");
    }

    private Div buildHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("flex-shrink", "0").set("margin-bottom", "16px");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        H2 title = new H2(getTranslation("admin.management.headerTitle"));
        title.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-header-text-color)");

        Paragraph subtitle = new Paragraph(getTranslation("admin.management.headerSubtitle"));
        subtitle.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

        textLayout.add(title, subtitle);

        Button resetDemoBtn = new Button(getTranslation("admin.management.btn.resetDemo"), VaadinIcon.TRASH.create());
        resetDemoBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Dialog confirmResetDialog = new Dialog();
        confirmResetDialog.setHeaderTitle(getTranslation("admin.management.dialog.resetTitle"));
        confirmResetDialog.add(new Paragraph(getTranslation("admin.management.dialog.resetText")));
        
        Button confirmDemoBtn = new Button(getTranslation("admin.management.btn.yesReset"), e -> {
            demoDataService.resetSystemForDemo();
            String admin = SecurityContextHolder.getContext().getAuthentication().getName();
            systemLogService.log("Admin (" + admin + ") sistemi DEMO modunda sıfırladı.");
            
            Notification.show(getTranslation("admin.management.notification.resetSuccess"), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            confirmResetDialog.close();
            refreshGrid();
        });
        confirmDemoBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        
        Button cancelDemoBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> confirmResetDialog.close());
        confirmResetDialog.getFooter().add(confirmDemoBtn, cancelDemoBtn);
        resetDemoBtn.addClickListener(e -> confirmResetDialog.open());

        Button exportBtn = new Button(getTranslation("admin.management.btn.excel"), VaadinIcon.FILE_TABLE.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        
        StreamResource resource = new StreamResource(
            "Talep_Raporu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx", 
            () -> excelExportService.exportRequestsToExcel(requestRepository.findAll())
        );
        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(exportBtn);

        Button pdfExportBtn = new Button(getTranslation("admin.management.btn.pdf"), VaadinIcon.FILE_TEXT_O.create());
        pdfExportBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        
        StreamResource pdfResource = new StreamResource(
            "Talep_Raporu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf", 
            () -> pdfExportService.exportRequestsToPdf(requestRepository.findAll())
        );
        Anchor pdfDownloadLink = new Anchor(pdfResource, "");
        pdfDownloadLink.getElement().setAttribute("download", true);
        pdfDownloadLink.add(pdfExportBtn);

        HorizontalLayout actionButtons = new HorizontalLayout(resetDemoBtn, downloadLink, pdfDownloadLink);
        actionButtons.setSpacing(true);
        actionButtons.setAlignItems(FlexComponent.Alignment.CENTER);

        titleRow.add(textLayout, actionButtons);
        header.add(titleRow);

        return header;
    }

    private void configureBulkActionBar() {
        bulkActionBar.setVisible(false);
        bulkActionBar.setWidthFull();
        bulkActionBar.setAlignItems(FlexComponent.Alignment.CENTER);
        bulkActionBar.getStyle()
            .set("background-color", "var(--lumo-primary-color-10pct)")
            .set("padding", "8px 16px")
            .set("border-radius", "8px")
            .set("margin-bottom", "12px");

        selectedCountLabel.getStyle()
            .set("font-weight", "bold")
            .set("color", "var(--lumo-primary-text-color)")
            .set("margin-right", "auto"); 

        Button bulkStatusBtn = new Button(getTranslation("admin.management.bulk.changeStatus"), VaadinIcon.EXCHANGE.create());
        bulkStatusBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        bulkStatusBtn.addClickListener(e -> bulkStatusDialog.open());

        Button bulkCloseBtn = new Button(getTranslation("admin.management.bulk.closeReject"), VaadinIcon.CLOSE_CIRCLE.create());
        bulkCloseBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        bulkCloseBtn.addClickListener(e -> bulkCloseDialog.open());

        bulkActionBar.add(selectedCountLabel, bulkStatusBtn, bulkCloseBtn);
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
            refreshGrid();
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
                refreshGrid();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> bulkStatusDialog.close());
        bulkStatusDialog.add(bulkStatusCombo);
        bulkStatusDialog.getFooter().add(saveBtn, cancelBtn);
    }

    private void configureGrid() {
        grid.addColumn(RequestEntity::getRequestId).setHeader(getTranslation("admin.grid.id")).setAutoWidth(true).setFlexGrow(0);
        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle).setHeader(getTranslation("admin.grid.title")).setWidth("150px").setResizable(true);
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription).setHeader(getTranslation("admin.grid.description")).setWidth("200px").setResizable(true);
        Grid.Column<RequestEntity> statusCol = grid.addComponentColumn(this::createStatusBadge).setHeader(getTranslation("admin.grid.status")).setAutoWidth(true).setFlexGrow(0);
        Grid.Column<RequestEntity> dateCol = grid.addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-").setHeader(getTranslation("admin.grid.date")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(this::createRatingColumn).setHeader(getTranslation("admin.grid.rating")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.getThemeList().add("spacing-s"); 

            Button sendToPoBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
            sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            sendToPoBtn.getElement().setProperty("title", getTranslation("admin.action.sendToPo"));
            sendToPoBtn.addClickListener(e -> forwardToPo(request));

            Button poBtn = new Button(VaadinIcon.SLIDERS.create());
            poBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            poBtn.getElement().setProperty("title", getTranslation("admin.action.prioritize"));
            poBtn.addClickListener(e -> {
                selectedRequestForPo = request;
                poDialog.open();
            });

            Button statusBtn = new Button(VaadinIcon.EXCHANGE.create());
            statusBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            statusBtn.getElement().setProperty("title", getTranslation("admin.action.changeStatus"));
            statusBtn.addClickListener(e -> {
                selectedRequestForStatus = request;
                statusCombo.setValue(request.getStatus());
                statusDialog.open();
            });

            Button closeBtn = new Button(VaadinIcon.CLOSE_CIRCLE.create());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            closeBtn.getElement().setProperty("title", getTranslation("admin.action.close"));
            closeBtn.addClickListener(e -> {
                selectedRequestForClose = request;
                closeDialog.open();
            });

            actions.add(sendToPoBtn, poBtn, statusBtn, closeBtn);
            return actions;
        }).setHeader(getTranslation("admin.grid.actions")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(this::createChatButton).setHeader(getTranslation("admin.grid.chat")).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create());
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            historyBtn.getElement().setProperty("title", getTranslation("admin.action.history"));
            historyBtn.addClickListener(e -> openHistoryDialog(request));
            return historyBtn;
        }).setHeader(getTranslation("admin.grid.log")).setAutoWidth(true).setFlexGrow(0);
        
        grid.addComponentColumn(this::createScreenshotButton).setHeader(getTranslation("admin.grid.screenshot")).setAutoWidth(true).setFlexGrow(0);

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(titleCol).setComponent(createFilterHeader(getTranslation("admin.filter.title"), requestFilter::setTitle));
        headerRow.getCell(descCol).setComponent(createFilterHeader(getTranslation("admin.filter.description"), requestFilter::setDescription));
        headerRow.getCell(statusCol).setComponent(createFilterHeader(getTranslation("admin.filter.status"), requestFilter::setStatus));
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));
    }

    private Component createRatingColumn(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Button rateBtn = new Button();
            if (request.getSatisfactionScore() != null) {
                rateBtn.setText("⭐ " + request.getSatisfactionScore());
                rateBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                    rateBtn.getElement().setProperty("title", getTranslation("admin.rating.commentTitle", request.getSatisfactionComment()));
                }
            } else {
                rateBtn.setText(getTranslation("admin.rating.rateBtn"));
                rateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            }
            rateBtn.addClickListener(e -> openRatingEditDialog(request));
            return rateBtn;
        }
        return new Span("-");
    }

    private void openRatingEditDialog(RequestEntity request) {
        Dialog ratingDialog = new Dialog();
        ratingDialog.setHeaderTitle(getTranslation("admin.ratingDialog.title"));
        ratingDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        RadioButtonGroup<Integer> scoreGroup = new RadioButtonGroup<>();
        scoreGroup.setLabel(getTranslation("admin.ratingDialog.scoreLabel"));
        scoreGroup.setItems(1, 2, 3, 4, 5);
        scoreGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        if (request.getSatisfactionScore() != null) {
            scoreGroup.setValue(request.getSatisfactionScore());
        }
        
        TextArea commentArea = new TextArea(getTranslation("admin.ratingDialog.commentLabel"));
        commentArea.setWidthFull();
        if (request.getSatisfactionComment() != null) {
            commentArea.setValue(request.getSatisfactionComment());
        }

        layout.add(scoreGroup, commentArea);
        ratingDialog.add(layout);

        Button submitBtn = new Button(getTranslation("admin.management.btn.save"), event -> {
            if (scoreGroup.getValue() == null) {
                Notification.show(getTranslation("admin.ratingDialog.errorScore"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            request.setSatisfactionScore(scoreGroup.getValue());
            request.setSatisfactionComment(commentArea.getValue());
            requestRepository.save(request);

            String admin = SecurityContextHolder.getContext().getAuthentication().getName();
            systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebinin puanını " + scoreGroup.getValue() + " yaptı.");

            Notification.show(getTranslation("admin.ratingDialog.success"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            ratingDialog.close();
            dataView.refreshItem(request); 
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> ratingDialog.close());

        ratingDialog.getFooter().add(submitBtn, cancelBtn);
        ratingDialog.open();
    }

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button(VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        chatButton.getElement().setProperty("title", getTranslation("admin.action.chatTitle"));
        
        Div container = new Div(chatButton);
        container.getStyle().set("position", "relative").set("display", "inline-block");

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(currentUser -> {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                Span badge = new Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.getStyle().set("position", "absolute").set("top", "-5px").set("right", "-5px")
                        .set("padding", "2px 5px").set("font-size", "10px").set("font-weight", "bold");
                container.add(badge);
            }
        });

        chatButton.addClickListener(e -> {
            systemLogService.log("Admin (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine giriş yaptı.");
            UI.getCurrent().navigate(TalepChat.class, request.getRequestId());
        });
        return container;
    }

    private void configureCloseDialog() {
        closeDialog.setHeaderTitle(getTranslation("admin.closeDialog.title"));
        closeReason.setLabel(getTranslation("admin.closeDialog.reasonLabel"));
        closeReason.setWidthFull();

        Button confirmBtn = new Button(getTranslation("admin.closeDialog.confirm"), e -> {
            if (selectedRequestForClose != null) {
                selectedRequestForClose.setStatus("KAPATILDI");
                requestRepository.save(selectedRequestForClose);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForClose.getRequestId() + " talebini kapattı. Gerekçe: " + closeReason.getValue());
                Notification.show(getTranslation("admin.closeDialog.success"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                closeDialog.close();
                closeReason.clear();
                refreshGrid();
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> closeDialog.close());

        closeDialog.getFooter().add(confirmBtn, cancelBtn);
        closeDialog.add(closeReason);
    }

    private void forwardToPo(RequestEntity request) {
        request.setStatus("INCELEMEDE");
        requestRepository.save(request);
        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebini PO'ya sevk etti.");
        Notification.show(getTranslation("admin.notification.poForwarded"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }

    private void configureStatusDialog() {
        statusDialog.setHeaderTitle(getTranslation("admin.statusDialog.title"));
        statusCombo.setLabel(getTranslation("admin.statusDialog.label"));
        statusCombo.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        statusCombo.setWidthFull();

        Button saveBtn = new Button(getTranslation("admin.management.btn.save"), e -> {
            if (selectedRequestForStatus != null && statusCombo.getValue() != null) {
                selectedRequestForStatus.setStatus(statusCombo.getValue());
                requestRepository.save(selectedRequestForStatus);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForStatus.getRequestId() + " durumunu '" + statusCombo.getValue() + "' yaptı.");
                Notification.show(getTranslation("admin.statusDialog.success"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                statusDialog.close();
                refreshGrid();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> statusDialog.close());

        statusDialog.add(statusCombo);
        statusDialog.getFooter().add(saveBtn, cancelBtn);
    }

    private void configurePoDialog() {
        poDialog.setHeaderTitle(getTranslation("admin.poDialog.title"));
        impact.setLabel(getTranslation("admin.poDialog.impact"));
        impact.setItems("1 - Çok Düşük", "2 - Düşük", "3 - Orta", "4 - Yüksek", "5 - Kritik");
        
        urgency.setLabel(getTranslation("admin.poDialog.urgency"));
        urgency.setItems("1 - Çok Düşük", "2 - Düşük", "3 - Orta", "4 - Yüksek", "5 - Çok Acil");
        
        effort.setLabel(getTranslation("admin.poDialog.effort"));
        effort.setItems("1 - Çok Kısa", "2 - Kısa", "3 - Orta", "4 - Uzun", "5 - Çok Uzun");
        
        securityOverride.setLabel(getTranslation("admin.poDialog.securityOverride"));
        securityOverride.getStyle().set("margin-top", "15px").set("font-weight", "bold").set("color", "var(--lumo-error-text-color)");
        
        FormLayout fl = new FormLayout(impact, urgency, effort);
        fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button scoreBtn = new Button(getTranslation("admin.poDialog.scoreBtn"), e -> {
            if (selectedRequestForPo != null) {
                boolean secOverride = securityOverride.getValue();
                if (!secOverride && (urgency.getValue() == null || impact.getValue() == null || effort.getValue() == null)) {
                    Notification.show(getTranslation("admin.poDialog.errorSelect"), 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                try {
                    int urg = urgency.getValue() != null ? Character.getNumericValue(urgency.getValue().charAt(0)) : 5;
                    int imp = impact.getValue() != null ? Character.getNumericValue(impact.getValue().charAt(0)) : 5;
                    int eff = effort.getValue() != null ? Character.getNumericValue(effort.getValue().charAt(0)) : 1;
                    
                    requestService.prioritizeRequest(selectedRequestForPo.getRequestId(), urg, imp, eff, secOverride);
                    String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                    systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForPo.getRequestId() + " talebini ONAYLANDI yaptı.");
                    Notification.show(getTranslation("admin.poDialog.successPrioritized"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    poDialog.close();
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show(getTranslation("admin.poDialog.errorPrefix") + ex.getMessage(), 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        scoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button toWorkflowBtn = new Button(getTranslation("admin.poDialog.toWorkflow"), e -> {
            if (selectedRequestForPo != null) {
                try {
                    requestService.goreveDonustur(selectedRequestForPo);
                    String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                    systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForPo.getRequestId() + " talebini YAZILIM GÖREVİNE dönüştürdü.");
                    Notification.show(getTranslation("admin.poDialog.successWorkflow"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    poDialog.close();
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        toWorkflowBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> poDialog.close());

        poDialog.add(fl, securityOverride);
        poDialog.getFooter().add(scoreBtn, toWorkflowBtn, cancelBtn);
    }

    private void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle(getTranslation("admin.historyDialog.title", request.getRequestId()));
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());
            if (logs == null || logs.isEmpty()) {
                layout.add(new Span(getTranslation("admin.historyDialog.empty")));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.getStyle().set("border-left", "3px solid var(--lumo-primary-color)").set("padding-left", "15px").set("margin-bottom", "15px");
                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : getTranslation("admin.historyDialog.unknownDate");
                    Span dateSpan = new Span(dateStr);
                    dateSpan.getStyle().set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)").set("display", "block").set("font-weight", "bold");
                    Span actionSpan = new Span(log.getAction());
                    actionSpan.getStyle().set("display", "block").set("margin-top", "4px");
                    stepItem.add(dateSpan, actionSpan);
                    layout.add(stepItem);
                }
            }
        } catch (Exception e) {
            layout.add(new Span(getTranslation("admin.historyDialog.errorPrefix") + e.getMessage()));
        }

        historyDialog.add(layout);
        historyDialog.getFooter().add(new Button(getTranslation("admin.closeDialog.closeBtn"), e -> historyDialog.close()));
        historyDialog.open();
    }

    private Component createScreenshotButton(RequestEntity request) {
        boolean hasScreenshot = request.getScreenshotData() != null && request.getScreenshotData().length > 0;
        
        Button button = new Button(VaadinIcon.PICTURE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.getElement().setProperty("title", hasScreenshot ? getTranslation("admin.screenshot.view") : getTranslation("admin.screenshot.none"));
        
        if (!hasScreenshot) {
            button.setEnabled(false);
            return button;
        }
        button.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle(getTranslation("admin.screenshot.dialogTitle", request.getRequestId()));
            StreamResource res = new StreamResource("img.png", () -> new ByteArrayInputStream(request.getScreenshotData()));
            Image img = new Image(res, getTranslation("admin.grid.screenshot"));
            img.setWidthFull();
            dialog.add(img);
            dialog.getFooter().add(new Button(getTranslation("admin.closeDialog.closeBtn"), ev -> dialog.close()));
            dialog.open();
        });
        return button;
    }

    private void refreshGrid() {
        if (dataView == null) {
            dataView = grid.setItems(requestRepository.findAll());
            requestFilter.setDataView(dataView);
        } else {
            dataView.refreshAll();
        }
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(status);
        switch (status) {
            case "NEW": badge.addThemeVariants(BadgeVariant.CONTRAST); break;
            case "INCELEMEDE": badge.addThemeVariants(BadgeVariant.WARNING); break;
            case "ONAYLANDI": 
            case "İş Akışına Dönüştü": badge.addThemeVariants(BadgeVariant.SUCCESS); break;
            case "KAPATILDI": badge.addThemeVariants(BadgeVariant.ERROR); break;
            default: badge.addThemeVariants(BadgeVariant.CONTRAST); break;
        }
        return badge;
    }

    private static Component createFilterHeader(String placeholder, Consumer<String> filterChangeConsumer) {
        TextField tf = new TextField();
        tf.setPlaceholder(placeholder);
        tf.setValueChangeMode(ValueChangeMode.EAGER);
        tf.setClearButtonVisible(true);
        tf.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tf.setWidthFull();
        tf.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return tf;
    }

    private static Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false); dateLayout.setSpacing(false);
        
        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder("Start / Başlangıç");
        startPicker.setWidth("110px");
        startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        startPicker.addValueChangeListener(e -> requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null));

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder("End / Bitiş");
        endPicker.setWidth("110px");
        endPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        endPicker.addValueChangeListener(e -> requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null));

        dateLayout.add(startPicker, endPicker);
        return dateLayout;
    }

    private static class RequestFilter {
        private GridListDataView<RequestEntity> dataView;
        private String title = "";
        private String description = "";
        private String status = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public void setDataView(GridListDataView<RequestEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setTitle(String t) { this.title = t; refresh(); }
        public void setDescription(String d) { this.description = d; refresh(); }
        public void setStatus(String s) { this.status = s; refresh(); }
        public void setStartDate(LocalDateTime s) { this.startDate = s; refresh(); }
        public void setEndDate(LocalDateTime e) { this.endDate = e; refresh(); }

        private void refresh() { if (dataView != null) dataView.refreshAll(); }

        public boolean test(RequestEntity request) {
            boolean mTitle = matches(request.getTitle(), title);
            boolean mDesc = matches(request.getDescription(), description);
            boolean mStatus = matches(request.getStatus(), status);
            boolean mDate = true;
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) mDate = false;
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) mDate = false;
            }
            return mTitle && mDesc && mStatus && mDate;
        }

        private boolean matches(String val, String search) {
            if (search == null || search.isEmpty()) return true;
            return val != null && val.toLowerCase().contains(search.toLowerCase());
        }
    }
}