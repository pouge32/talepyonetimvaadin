package com.example.base.ui.AdminScreen.AdminManagment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.ChatService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Chat.TalepChat;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

public class AdminGridComponent extends Grid<RequestEntity> {

    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final SystemLogService systemLogService;
    private final AdminDialogsHelper dialogsHelper;
    private final Consumer<RequestEntity> onForwardToPoAction;
    private final AdminRequestFilter requestFilter = new AdminRequestFilter();

    public AdminGridComponent(WorkflowRepository workflowRepository, UserRepository userRepository,
                              ChatService chatService, SystemLogService systemLogService,
                              AdminDialogsHelper dialogsHelper, Consumer<RequestEntity> onForwardToPoAction) {
        super(RequestEntity.class, false);
        this.workflowRepository = workflowRepository;
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.systemLogService = systemLogService;
        this.dialogsHelper = dialogsHelper;
        this.onForwardToPoAction = onForwardToPoAction;

        setWidthFull();
        addClassName("admin-grid");
        addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES); 

        configureGrid();
    }

    public void setGridItems(List<RequestEntity> items) {
        var dataView = this.setItems(items);
        requestFilter.setDataView(dataView);
    }

    private void configureGrid() {
        addColumn(RequestEntity::getRequestId).setHeader(getTranslation("admin.grid.id")).setWidth("60px").setFlexGrow(0);
        Column<RequestEntity> titleCol = addColumn(RequestEntity::getTitle).setHeader(getTranslation("admin.grid.title")).setWidth("120px").setFlexGrow(1).setResizable(true);
        Column<RequestEntity> descCol = addColumn(RequestEntity::getDescription).setHeader(getTranslation("admin.grid.description")).setWidth("120px").setFlexGrow(1).setResizable(true);
        Column<RequestEntity> statusCol = addComponentColumn(this::createStatusBadge).setHeader(getTranslation("admin.grid.status")).setWidth("135px").setFlexGrow(0);
        Column<RequestEntity> priorityCol = addComponentColumn(this::createPriorityBadge).setHeader(getTranslation("programmer.grid.priority")).setWidth("125px").setFlexGrow(0);
        
        Column<RequestEntity> effortCol = addColumn(request -> {
            try {
                WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
                return (workflow != null && workflow.getActualEffortHours() != null) ? workflow.getActualEffortHours() + " S" : "-";
            } catch (Exception e) { return "-"; }
        }).setHeader("Efor").setWidth("75px").setFlexGrow(0);

        Column<RequestEntity> dateCol = addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) : "-").setHeader(getTranslation("admin.grid.date")).setWidth("110px").setFlexGrow(0);
        addComponentColumn(this::createRatingColumn).setHeader(getTranslation("admin.grid.rating")).setWidth("80px").setFlexGrow(0);

        addComponentColumn(request -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.getThemeList().add("spacing-s"); 

            Button sendToPoBtn = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> onForwardToPoAction.accept(request));
            sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            
            Button poBtn = new Button(VaadinIcon.SLIDERS.create(), e -> dialogsHelper.openPoDialog(request));
            poBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            
            Button statusBtn = new Button(VaadinIcon.EXCHANGE.create(), e -> dialogsHelper.openStatusDialog(request));
            statusBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

            Button closeBtn = new Button(VaadinIcon.CLOSE_CIRCLE.create(), e -> dialogsHelper.openCloseDialog(request));
            closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            actions.add(sendToPoBtn, poBtn, statusBtn, closeBtn);
            return actions;
        }).setHeader(getTranslation("admin.grid.actions")).setWidth("190px").setFlexGrow(0);

        addComponentColumn(this::createChatButton).setHeader(getTranslation("admin.grid.chat")).setWidth("60px").setFlexGrow(0);
        
        addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create(), e -> dialogsHelper.openHistoryDialog(request));
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return historyBtn;
        }).setHeader(getTranslation("admin.grid.log")).setWidth("60px").setFlexGrow(0);
        
        addComponentColumn(req -> {
            boolean hasScreenshot = req.getScreenshotData() != null && req.getScreenshotData().length > 0;
            Button btn = new Button(VaadinIcon.PICTURE.create(), e -> dialogsHelper.openScreenshotDialog(req));
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            btn.setEnabled(hasScreenshot);
            return btn;
        }).setHeader(getTranslation("admin.grid.screenshot")).setWidth("65px").setFlexGrow(0);

        addItemDoubleClickListener(e -> { if (e.getItem() != null) dialogsHelper.openRequestDetailDialog(e.getItem()); });

        HeaderRow headerRow = appendHeaderRow();
        headerRow.getCell(titleCol).setComponent(createFilterHeader(getTranslation("admin.filter.title"), requestFilter::setTitle));
        headerRow.getCell(descCol).setComponent(createFilterHeader(getTranslation("admin.filter.description"), requestFilter::setDescription));
        headerRow.getCell(statusCol).setComponent(createStatusFilterHeader());
        headerRow.getCell(priorityCol).setComponent(createPriorityFilterHeader());
        headerRow.getCell(effortCol).setComponent(new Span()); 
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader());
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        String translated = getTranslatedStatus(status);
        Badge badge = new Badge(translated);
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

    private Span createPriorityBadge(RequestEntity request) {
        Span badge = new Span();
        if (request.getPrioritization() != null) {
            int score = request.getPrioritization().getPriorityScore();
            if (score >= 999) { badge.setText(getTranslation("programmer.combobox.Priority.urgent")); badge.getElement().getThemeList().add("badge error"); }
            else if (score >= 20) { badge.setText(getTranslation("programmer.combobox.Priority.critical")); badge.getElement().getThemeList().add("badge error primary"); }
            else if (score >= 10) { badge.setText(getTranslation("programmer.combobox.Priority.high")); badge.getElement().getThemeList().add("badge warning"); }
            else if (score >= 5) { badge.setText(getTranslation("programmer.combobox.Priority.normal")); badge.getElement().getThemeList().add("badge success"); }
            else { badge.setText(getTranslation("admin.priority.low", "DÜŞÜK")); badge.getElement().getThemeList().add("badge contrast"); }
        } else {
            badge.setText("-"); badge.getElement().getThemeList().add("badge contrast");
        }
        badge.addClassName("admin-priority-badge");
        return badge;
    }

    private Component createRatingColumn(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Button rateBtn = new Button();
            if (request.getSatisfactionScore() != null) {
                rateBtn.setText("⭐ " + request.getSatisfactionScore());
                rateBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            } else {
                rateBtn.setText(getTranslation("admin.rating.rateBtn"));
                rateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            }
            rateBtn.addClickListener(e -> dialogsHelper.openRatingEditDialog(request));
            return rateBtn;
        }
        return new Span("-");
    }

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button(VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Div container = new Div(chatButton);
        container.addClassName("admin-chat-container");
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(currentUser -> {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                Span badge = new Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.addClassName("admin-chat-badge");
                container.add(badge);
            }
        });
        chatButton.addClickListener(e -> {
            systemLogService.log("Admin (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine giriş yaptı.");
            UI.getCurrent().navigate(TalepChat.class, request.getRequestId());
        });
        return container;
    }

    private Component createFilterHeader(String placeholder, Consumer<String> filterChangeConsumer) {
        TextField tf = new TextField(); tf.setPlaceholder(placeholder); tf.setValueChangeMode(ValueChangeMode.EAGER);
        tf.setClearButtonVisible(true); tf.addThemeVariants(TextFieldVariant.LUMO_SMALL); tf.setWidthFull();
        tf.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue())); return tf;
    }

    private Component createPriorityFilterHeader() {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setItems(999, 20, 10, 5, 0);
        comboBox.setItemLabelGenerator(score -> {
            if (score >= 999) return getTranslation("programmer.combobox.Priority.urgent");
            if (score >= 20) return getTranslation("programmer.combobox.Priority.critical");
            if (score >= 10) return getTranslation("programmer.combobox.Priority.high");
            if (score >= 5) return getTranslation("programmer.combobox.Priority.normal");
            return getTranslation("admin.priority.low", "DÜŞÜK");
        });
        comboBox.setPlaceholder(getTranslation("programmer.grid.priority"));
        comboBox.setClearButtonVisible(true); comboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL); comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> requestFilter.setMinScoreFilter(e.getValue())); return comboBox;
    }

    private Component createStatusFilterHeader() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        comboBox.setItemLabelGenerator(this::getTranslatedStatus);
        comboBox.setPlaceholder(getTranslation("admin.filter.status"));
        comboBox.setClearButtonVisible(true); comboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL); comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> requestFilter.setStatus(e.getValue())); return comboBox;
    }

    private Component createDateRangeFilterHeader() {
        VerticalLayout dateLayout = new VerticalLayout(); dateLayout.setPadding(false); dateLayout.setSpacing(false);
        DatePicker startPicker = new DatePicker(); startPicker.setPlaceholder(getTranslation("requests.filter.startDate"));
        startPicker.setWidth("95px"); startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        startPicker.setValue(LocalDate.now().minusWeeks(1));
        DatePicker endPicker = new DatePicker(); endPicker.setPlaceholder(getTranslation("requests.filter.endDate"));
        endPicker.setWidth("95px"); endPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        endPicker.setValue(LocalDate.now());
        startPicker.addValueChangeListener(e -> requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null));
        endPicker.addValueChangeListener(e -> requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null));
        dateLayout.add(startPicker, endPicker); return dateLayout;
    }

    private String getTranslatedStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "NEW": return getTranslation("requests.status.new");
            case "INCELEMEDE": return getTranslation("requests.status.inReview");
            case "ONAYLANDI": case "İş Akışına Dönüştü": return getTranslation("requests.status.inProgress");
            case "KAPATILDI": return getTranslation("requests.status.closed");
            default: return status;
        }
    }
}