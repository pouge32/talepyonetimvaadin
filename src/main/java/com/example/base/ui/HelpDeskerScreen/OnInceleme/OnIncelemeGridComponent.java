package com.example.base.ui.HelpDeskerScreen.OnInceleme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.ChatService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Chat.TalepChat;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

public class OnIncelemeGridComponent extends Grid<RequestEntity> {

    private final ChatService chatService;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;
    private final WorkflowRepository workflowRepository;
    private final OnIncelemeDialogHelper dialogHelper;
    private final UserEntity currentUser;
    private final Consumer<RequestEntity> onForwardToPo;

    public OnIncelemeGridComponent(ChatService chatService, SystemLogService systemLogService,
                                   SettingsService settingsService, WorkflowRepository workflowRepository,
                                   OnIncelemeDialogHelper dialogHelper, UserEntity currentUser,
                                   Consumer<RequestEntity> onForwardToPo) {
        super(RequestEntity.class, false);
        this.chatService = chatService;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
        this.workflowRepository = workflowRepository;
        this.dialogHelper = dialogHelper;
        this.currentUser = currentUser;
        this.onForwardToPo = onForwardToPo;

        setWidthFull();
        configureColumns();
    }

    public void setGridItems(List<RequestEntity> requests, OnIncelemeFilter requestFilter) {
        GridListDataView<RequestEntity> dataView = this.setItems(requests);
        requestFilter.setDataView(dataView);
        setupFilterRow(requestFilter);
    }

    private void configureColumns() {
        addColumn(RequestEntity::getTitle).setHeader(getTranslation("helpdesk.triage.grid.title")).setFlexGrow(1).setKey("title");
        addColumn(RequestEntity::getDescription).setHeader(getTranslation("helpdesk.triage.grid.desc")).setFlexGrow(2).setKey("desc");

        addColumn(req -> {
            try {
                return req.getAssignedUser() != null ? req.getAssignedUser().getNameSurname() : getTranslation("helpdesk.triage.unassigned");
            } catch (Exception e) {
                return getTranslation("helpdesk.triage.unassigned");
            }
        }).setHeader("Uzman").setWidth("120px").setFlexGrow(0).setKey("assigned");

        addComponentColumn(this::createStatusBadge).setHeader(getTranslation("helpdesk.triage.grid.status")).setWidth("140px").setFlexGrow(0).setKey("status");

        addColumn(request -> {
            try {
                WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
                if (workflow != null && workflow.getActualEffortHours() != null) {
                    return workflow.getActualEffortHours() + " S"; 
                }
            } catch (Exception e) {}
            return "-";
        }).setHeader("Efor").setWidth("75px").setFlexGrow(0).setKey("effort");

        addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) : "-")
                .setHeader("Tarih").setWidth("185px").setFlexGrow(0).setKey("date");

        addComponentColumn(this::createScreenshotButton).setHeader("Görsel").setWidth("80px").setFlexGrow(0).setKey("screen");
        addComponentColumn(this::createChatButton).setHeader("Sohbet").setWidth("75px").setFlexGrow(0).setKey("chat");

        addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create());
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            historyBtn.getElement().setProperty("title", "Geçmişi Gör");
            historyBtn.addClickListener(e -> dialogHelper.openHistoryDialog(request));
            return historyBtn;
        }).setHeader("Geçmiş").setWidth("80px").setFlexGrow(0).setKey("history");

        addComponentColumn(this::createRatingColumn).setHeader("Puan").setWidth("75px").setFlexGrow(0).setKey("rating");

        addComponentColumn(request -> {
            HorizontalLayout actLayout = new HorizontalLayout();
            actLayout.setSpacing(true); actLayout.setPadding(false);

            if ("NEW".equals(request.getStatus())) {
                Button closeBtn = new Button(VaadinIcon.CLOSE_CIRCLE.create());
                closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                closeBtn.getElement().setProperty("title", getTranslation("helpdesk.triage.btn.closeRequest"));
                closeBtn.addClickListener(e -> dialogHelper.openCloseDialog(request));

                Button sendToPoBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
                sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                sendToPoBtn.getElement().setProperty("title", getTranslation("helpdesk.triage.btn.forwardPo"));
                sendToPoBtn.addClickListener(e -> onForwardToPo.accept(request));

                actLayout.add(closeBtn, sendToPoBtn);
            } else if ("DESTEK_KONTROL".equals(request.getStatus())) {
                Button confirmBtn = new Button("Teyit", VaadinIcon.PHONE.create());
                confirmBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                confirmBtn.addClickListener(e -> dialogHelper.openCustomerConfirmationDialog(request));
                actLayout.add(confirmBtn);
            } else {
                actLayout.add(new Span("-"));
            }
            return actLayout;
        }).setHeader("İşlem").setWidth("100px").setFlexGrow(0).setKey("action");

        addComponentColumn(this::createSlaBadge).setHeader("SLA").setWidth("105px").setFlexGrow(0).setKey("sla");

        addItemDoubleClickListener(event -> {
            if (event.getItem() != null) dialogHelper.openRequestDetailDialog(event.getItem());
        });
    }

    private void setupFilterRow(OnIncelemeFilter requestFilter) {
        if (getHeaderRows().size() > 1) {
            removeHeaderRow(getHeaderRows().get(1));
        }

        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("helpdesk.triage.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> requestFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = appendHeaderRow();
        headerRow.getCell(getColumnByKey("title")).setComponent(searchField);
        headerRow.getCell(getColumnByKey("desc")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("assigned")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("status")).setComponent(createStatusFilterHeader(requestFilter::setStatus));
        headerRow.getCell(getColumnByKey("effort")).setComponent(new Span()); 
        headerRow.getCell(getColumnByKey("date")).setComponent(createDateRangeFilterHeader(requestFilter));
        headerRow.getCell(getColumnByKey("screen")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("chat")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("history")).setComponent(new Span()); 
        headerRow.getCell(getColumnByKey("rating")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("action")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("sla")).setComponent(new Span());
    }

    private Component createDateRangeFilterHeader(OnIncelemeFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false); dateLayout.setSpacing(false);
        dateLayout.addClassName("on-inceleme-date-layout");

        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder("İlk");
        startPicker.setClearButtonVisible(true);
        startPicker.setWidth("85px"); 
        startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        startPicker.setValue(LocalDate.now().minusWeeks(1)); 
        requestFilter.setStartDate(LocalDate.now().minusWeeks(1).atStartOfDay());

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder("Son");
        endPicker.setClearButtonVisible(true);
        endPicker.setWidth("85px"); 
        endPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        endPicker.setValue(LocalDate.now()); 
        requestFilter.setEndDate(LocalDate.now().atTime(23, 59, 59));

        startPicker.addValueChangeListener(e -> {
            endPicker.setMin(e.getValue());
            requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });
        endPicker.addValueChangeListener(e -> {
            startPicker.setMax(e.getValue());
            requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        HorizontalLayout layout = new HorizontalLayout(startPicker, endPicker);
        layout.setPadding(false); layout.setSpacing(false);
        layout.addClassName("on-inceleme-date-layout");
        return layout;
    }

    private Component createStatusFilterHeader(Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "DESTEK_KONTROL", "KAPATILDI");
        comboBox.setPlaceholder(getTranslation("requests.filter.statusPlaceholder"));
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button(VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Div container = new Div(chatButton);
        container.addClassName("on-inceleme-chat-container");

        if (currentUser != null) {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                Span badge = new Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.addClassName("on-inceleme-chat-badge");
                container.add(badge);
            }
        }

        chatButton.addClickListener(e -> {
            String email = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
            systemLogService.log("Destek Personeli (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine girdi.");
            e.getSource().getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId()));
        });
        return container;
    }

    private Component createScreenshotButton(RequestEntity request) {
        boolean hasScreenshot = request.getScreenshotData() != null && request.getScreenshotData().length > 0;
        Button button = new Button(hasScreenshot ? getTranslation("requests.btn.view") : getTranslation("requests.btn.none"), VaadinIcon.PICTURE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        if (!hasScreenshot) {
            button.setEnabled(false);
            button.getElement().setProperty("title", getTranslation("requests.tooltip.noScreenshot"));
            return button;
        }
        button.getElement().setProperty("title", getTranslation("requests.tooltip.viewScreenshot"));
        button.addClickListener(e -> dialogHelper.openScreenshotDialog(request));
        return button;
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(status);
        badge.addClassName("on-inceleme-status-badge");
        
        switch (status) {
            case "NEW": badge.addThemeVariants(BadgeVariant.CONTRAST); break;
            case "INCELEMEDE": badge.addThemeVariants(BadgeVariant.WARNING); break;
            case "DESTEK_KONTROL": 
                badge.addThemeVariants(BadgeVariant.SUCCESS); 
                badge.getElement().getThemeList().add("primary");
                break;
            case "ONAYLANDI": 
            case "İş Akışına Dönüştü": badge.addThemeVariants(BadgeVariant.SUCCESS); break;
            case "KAPATILDI": badge.addThemeVariants(BadgeVariant.ERROR); break;
            default: badge.addThemeVariants(BadgeVariant.CONTRAST); break;
        }
        return badge;
    }

    private Component createRatingColumn(RequestEntity request) {
        if (request.getSatisfactionScore() != null) {
            Span pointBadge = new Span("⭐ " + request.getSatisfactionScore() + "/5");
            pointBadge.getElement().getThemeList().add("badge success");
            pointBadge.addClassName("on-inceleme-point-badge");
            
            if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                pointBadge.getElement().setProperty("title", getTranslation("helpdesk.triage.commentPrefix") + ": " + request.getSatisfactionComment());
                pointBadge.addClassName("on-inceleme-point-badge-help");
            }
            return pointBadge;
        } else if ("KAPATILDI".equals(request.getStatus())) {
            return new Span(getTranslation("helpdesk.triage.unrated"));
        }
        return new Span("-");
    }

    private Badge createSlaBadge(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Badge closedBadge = new Badge(getTranslation("requests.sla.completed"));
            closedBadge.addThemeVariants(BadgeVariant.CONTRAST); 
            return closedBadge;
        }

        long hoursElapsed = java.time.temporal.ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());
        long slaLimitHours = settingsService.getSlaLimitHours();
        long warningLimitHours = (long) (slaLimitHours * settingsService.getSlaWarningPercent());

        if (hoursElapsed >= slaLimitHours) {
            Badge ihlalBadge = new Badge(getTranslation("requests.sla.violated") + " (" + hoursElapsed + "s)");
            ihlalBadge.addThemeVariants(BadgeVariant.ERROR);
            ihlalBadge.getElement().setProperty("title", getTranslation("requests.sla.violatedTitle"));
            return ihlalBadge;
        } else if (hoursElapsed >= warningLimitHours) {
            Badge uyariBadge = new Badge(getTranslation("requests.sla.warning") + " (" + hoursElapsed + "s)");
            uyariBadge.addThemeVariants(BadgeVariant.WARNING);
            uyariBadge.getElement().setProperty("title", getTranslation("requests.sla.warningTitle"));
            return uyariBadge;
        } else {
            Badge normalBadge = new Badge(getTranslation("requests.sla.normal") + " (" + hoursElapsed + "s)");
            normalBadge.addThemeVariants(BadgeVariant.SUCCESS);
            return normalBadge;
        }
    }
}