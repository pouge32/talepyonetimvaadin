package com.example.base.ui.CustomerScreen.Taleplerim;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.UserRepository;
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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

public class TaleplerimGridComponent extends Grid<RequestEntity> {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;
    private final WorkflowRepository workflowRepository;
    private final TaleplerimDialogHelper dialogHelper;

    public TaleplerimGridComponent(ChatService chatService, UserRepository userRepository,
                                   SystemLogService systemLogService, SettingsService settingsService,
                                   WorkflowRepository workflowRepository, TaleplerimDialogHelper dialogHelper) {
        super(RequestEntity.class, false);
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
        this.workflowRepository = workflowRepository;
        this.dialogHelper = dialogHelper;

        setWidthFull();
        addClassName("taleplerim-grid");
        configureColumns();
    }

    public void setGridItems(List<RequestEntity> requests) {
        var dataView = this.setItems(requests);
        TaleplerimFilter requestFilter = new TaleplerimFilter(dataView);

        LocalDateTime now = LocalDateTime.now();
        requestFilter.setStartDate(now.minusWeeks(1));
        requestFilter.setEndDate(now);

        setupFilterRow(requestFilter);
    }

    private void configureColumns() {
        addColumn(RequestEntity::getRequestId).setHeader(getTranslation("requests.grid.id")).setWidth("60px").setFlexGrow(0).setKey("id");
        addColumn(RequestEntity::getTitle).setHeader(getTranslation("requests.grid.title")).setFlexGrow(1).setKey("title");
        addColumn(RequestEntity::getDescription).setHeader(getTranslation("requests.grid.desc")).setFlexGrow(2).setKey("desc");
        
        addComponentColumn(this::createScreenshotButton).setHeader(getTranslation("requests.grid.screenshot")).setWidth("120px").setFlexGrow(0).setKey("screenshot");

        addColumn(request -> request.getCreatedAt() != null ? request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-")
                .setHeader(getTranslation("requests.grid.createdAt")).setWidth("130px").setFlexGrow(0).setKey("date");
                
        addComponentColumn(this::createStatusBadge).setHeader(getTranslation("requests.grid.status")).setWidth("150px").setFlexGrow(0).setKey("status");

        addColumn(request -> {
            try {
                WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
                if (workflow != null && workflow.getActualEffortHours() != null) {
                    return workflow.getActualEffortHours() + " Saat";
                }
            } catch (Exception e) {}
            return "-";
        }).setHeader("Harcanan Efor").setWidth("120px").setFlexGrow(0).setKey("effort");

        addComponentColumn(request -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.getThemeList().add("spacing-s");
            actions.setAlignItems(FlexComponent.Alignment.CENTER);

            actions.add(createChatButton(request));

            if ("KAPATILDI".equals(request.getStatus())) {
                Button reopenBtn = new Button(getTranslation("requests.btn.reopen"), VaadinIcon.REFRESH.create());
                reopenBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                reopenBtn.getElement().setProperty("title", getTranslation("requests.tooltip.reopen"));
                reopenBtn.addClickListener(e -> dialogHelper.openReopenDialog(request));
                actions.add(reopenBtn);
            }

            return actions;
        }).setHeader(getTranslation("requests.grid.actions")).setWidth("210px").setFlexGrow(0).setKey("actions");

        addComponentColumn(this::createRatingColumn).setHeader(getTranslation("requests.grid.rating")).setWidth("120px").setFlexGrow(0).setKey("rating");
        addComponentColumn(this::createSlaBadge).setHeader(getTranslation("requests.grid.sla")).setWidth("110px").setFlexGrow(0).setKey("sla");

        addItemDoubleClickListener(event -> {
            if (event.getItem() != null) dialogHelper.openRequestDetailDialog(event.getItem());
        });
    }

    private void setupFilterRow(TaleplerimFilter requestFilter) {
        if (getHeaderRows().size() > 1) {
            removeHeaderRow(getHeaderRows().get(1));
        }
        
        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("requests.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> requestFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = appendHeaderRow();
        headerRow.getCell(getColumnByKey("id")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("title")).setComponent(searchField);
        headerRow.getCell(getColumnByKey("desc")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("screenshot")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("date")).setComponent(createDateRangeFilterHeader(requestFilter));
        headerRow.getCell(getColumnByKey("status")).setComponent(createStatusFilterHeader(requestFilter::setStatus));
        headerRow.getCell(getColumnByKey("effort")).setComponent(new Span()); 
        headerRow.getCell(getColumnByKey("actions")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("rating")).setComponent(new Span());
        headerRow.getCell(getColumnByKey("sla")).setComponent(new Span());
    }

    private Component createDateRangeFilterHeader(TaleplerimFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false);
        dateLayout.setSpacing(false); 
        dateLayout.addClassName("taleplerim-date-layout");

        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder(getTranslation("requests.filter.startDate"));
        startPicker.setWidth("110px"); 
        startPicker.setClearButtonVisible(true);
        startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL); 
        startPicker.setValue(LocalDate.now().minusWeeks(1));
        startPicker.addValueChangeListener(e -> requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null));

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder(getTranslation("requests.filter.endDate"));
        endPicker.setWidth("110px"); 
        endPicker.setClearButtonVisible(true);
        endPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL); 
        endPicker.setValue(LocalDate.now());
        endPicker.addValueChangeListener(e -> requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null));

        dateLayout.add(startPicker, endPicker);
        return dateLayout;
    }

    private Component createStatusFilterHeader(Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(
            getTranslation("requests.status.new"), 
            getTranslation("requests.status.inReview"), 
            getTranslation("requests.status.inProgress"), 
            getTranslation("requests.status.closed")
        );
        comboBox.setPlaceholder(getTranslation("requests.filter.statusPlaceholder"));
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
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

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button(getTranslation("requests.btn.chat"), VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Div container = new Div(chatButton);
        container.addClassName("taleplerim-chat-container");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        
        userRepository.findByEmail(email).ifPresent(currentUser -> {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                Span badge = new Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.addClassName("taleplerim-chat-badge");
                container.add(badge);
            }
        });

        chatButton.addClickListener(e -> {
            systemLogService.log("Müşteri (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine giriş yaptı.");
            e.getSource().getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId()));
        });
        return container;
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(statusLabel(status));
        String tooltipText = "";

        switch (status) {
            case "NEW": badge.addThemeVariants(BadgeVariant.CONTRAST); tooltipText = getTranslation("requests.tooltip.statusNew"); break;
            case "INCELEMEDE":
            case "İncelemede": badge.addThemeVariants(BadgeVariant.WARNING); tooltipText = getTranslation("requests.tooltip.statusInReview"); break;
            case "ONAYLANDI":
            case "İş Akışına Dönüştü": badge.addThemeVariants(BadgeVariant.SUCCESS); tooltipText = getTranslation("requests.tooltip.statusInProgress"); break;
            case "KAPATILDI": badge.addThemeVariants(BadgeVariant.ERROR); tooltipText = getTranslation("requests.tooltip.statusClosed"); break;
            default: badge.addThemeVariants(BadgeVariant.ERROR); tooltipText = getTranslation("requests.tooltip.statusDefault"); break;
        }

        badge.getElement().setProperty("title", tooltipText);
        badge.addClassName("taleplerim-status-badge");
        return badge;
    }

    private Badge createSlaBadge(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Badge closedBadge = new Badge(getTranslation("requests.sla.completed"));
            closedBadge.addThemeVariants(BadgeVariant.CONTRAST); 
            return closedBadge;
        }

        long hoursElapsed = ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());
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

    private Component createRatingColumn(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            if (request.getSatisfactionScore() != null) {
                Span pointBadge = new Span("⭐ " + request.getSatisfactionScore() + "/5");
                pointBadge.getElement().getThemeList().add("badge success");
                pointBadge.addClassName("taleplerim-point-badge");
                return pointBadge;
            } else {
                Button rateBtn = new Button(getTranslation("requests.btn.rate"), VaadinIcon.STAR.create());
                rateBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                rateBtn.addClickListener(e -> dialogHelper.openRatingDialog(request));
                return rateBtn;
            }
        }
        return new Span("-");
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "NEW" -> getTranslation("requests.status.new");
            case "INCELEMEDE", "İncelemede" -> getTranslation("requests.status.inReview");
            case "ONAYLANDI", "İş Akışına Dönüştü" -> getTranslation("requests.status.inProgress");
            case "KAPATILDI" -> getTranslation("requests.status.closed");
            default -> status;
        };
    }
}