package com.example.base.ui.HelpDeskerScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatService;
import com.example.base.service.NotificationService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Class.TalepChat;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "on-inceleme", layout = MainLayout.class)
@RolesAllowed("HELPDESK")
public class OnIncelemeView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;
    
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    private GridListDataView<RequestEntity> dataView;
    private final RequestFilter requestFilter = new RequestFilter();
    
    private RequestEntity selectedRequest;
    private Dialog closeDialog = new Dialog();
    private TextArea closeReason = new TextArea();

    private UserEntity currentUser;

    public OnIncelemeView(RequestRepository requestRepository, NotificationService notificationService,
                          ChatService chatService, UserRepository userRepository,
                          SystemLogService systemLogService, SettingsService settingsService) {
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H3 title = new H3(getTranslation("helpdesk.triage.headerTitle"));
        title.getStyle().set("margin", "0");

        Tab tabMine = new Tab(getTranslation("helpdesk.triage.tab.assignedToMe"));
        Tab tabAll = new Tab(getTranslation("helpdesk.triage.tab.allPool"));
        Tabs tabs = new Tabs(tabMine, tabAll);
        
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(tabMine) && currentUser != null) {
                requestFilter.setAssignedUserIdFilter(currentUser.getUserId());
            } else {
                requestFilter.setAssignedUserIdFilter(null);
            }
        });

        headerLayout.add(title, tabs);
        add(headerLayout);

        if (currentUser != null) {
            requestFilter.setAssignedUserIdFilter(currentUser.getUserId());
        }

        configureGrid();
        configureCloseDialog();

        grid.setWidthFull();
        add(grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("helpdesk.triage.pageTitle");
    }

    private void configureGrid() {
        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle).setHeader(getTranslation("helpdesk.triage.grid.title"));
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription).setHeader(getTranslation("helpdesk.triage.grid.desc"));
        
        Grid.Column<RequestEntity> assignedCol = grid.addColumn(req -> {
            try {
                return req.getAssignedUser() != null ? req.getAssignedUser().getNameSurname() : getTranslation("helpdesk.triage.unassigned");
            } catch (Exception e) {
                return getTranslation("helpdesk.triage.unassigned");
            }
        }).setHeader(getTranslation("helpdesk.triage.grid.assignedUser")).setAutoWidth(true);

        Grid.Column<RequestEntity> statusCol = grid.addComponentColumn(this::createStatusBadge).setHeader(getTranslation("helpdesk.triage.grid.status")).setAutoWidth(true);
        Grid.Column<RequestEntity> dateCol = grid.addColumn(RequestEntity::getCreatedAt).setHeader(getTranslation("helpdesk.triage.grid.createdAt"));

        grid.addComponentColumn(this::createScreenshotButton)
                .setHeader(getTranslation("helpdesk.triage.grid.screenshot")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            Button chatButton = new Button(getTranslation("helpdesk.triage.btn.chat"), VaadinIcon.CHAT.create());
            chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Div container = new Div(chatButton);
            container.getStyle().set("position", "relative").set("display", "inline-block");

            if (currentUser != null) {
                int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
                if (unreadCount > 0) {
                    Span badge = new Span(String.valueOf(unreadCount));
                    badge.getElement().getThemeList().add("badge error primary pill");
                    badge.getStyle().set("position", "absolute").set("top", "-5px").set("right", "-5px")
                            .set("padding", "2px 6px").set("font-size", "10px").set("font-weight", "bold");
                    container.add(badge);
                }
            }

            chatButton.addClickListener(e -> {
                String email = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
                systemLogService.log("Destek Personeli (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine girdi.");
                e.getSource().getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId()));
            });
            return container;
        }).setHeader(getTranslation("helpdesk.triage.grid.chat")).setAutoWidth(true);

        grid.addComponentColumn(this::createRatingColumn).setHeader(getTranslation("helpdesk.triage.grid.rating")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            if (!"NEW".equals(request.getStatus())) {
                return new Span("-"); 
            }

            Button closeBtn = new Button(getTranslation("helpdesk.triage.btn.closeRequest"), VaadinIcon.CLOSE_CIRCLE.create());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            closeBtn.addClickListener(e -> {
                selectedRequest = request;
                closeDialog.open();
            });

            Button sendToPoBtn = new Button(getTranslation("helpdesk.triage.btn.forwardPo"), VaadinIcon.ARROW_RIGHT.create());
            sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            sendToPoBtn.addClickListener(e -> forwardToPo(request));

            return new HorizontalLayout(closeBtn, sendToPoBtn);
        }).setHeader(getTranslation("helpdesk.triage.grid.actions")).setAutoWidth(true);

        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("helpdesk.triage.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> requestFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(titleCol).setComponent(searchField);
        headerRow.getCell(descCol).setComponent(new Span());
        headerRow.getCell(assignedCol).setComponent(new Span());
        headerRow.getCell(statusCol).setComponent(createStatusFilterHeader(requestFilter::setStatus));
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));

        grid.addComponentColumn(this::createSlaBadge).setHeader(getTranslation("helpdesk.triage.grid.sla")).setAutoWidth(true).setFlexGrow(0);
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
        if (request.getSatisfactionScore() != null) {
            Span pointBadge = new Span("⭐ " + request.getSatisfactionScore() + "/5");
            pointBadge.getElement().getThemeList().add("badge success");
            pointBadge.getStyle().set("font-weight", "bold");
            
            if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                pointBadge.getElement().setProperty("title", getTranslation("helpdesk.triage.commentPrefix") + ": " + request.getSatisfactionComment());
                pointBadge.getStyle().set("cursor", "help");
            }
            return pointBadge;
        } else if ("KAPATILDI".equals(request.getStatus())) {
            return new Span(getTranslation("helpdesk.triage.unrated"));
        }
        return new Span("-");
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
        button.addClickListener(e -> openScreenshotDialog(request));
        return button;
    }

    private void openScreenshotDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("requests.dialog.screenshotTitle") + " #" + request.getRequestId());
        dialog.setWidth("640px");
        dialog.setCloseOnOutsideClick(true);

        String fileName = request.getScreenshotFileName() != null ? request.getScreenshotFileName() : "ekran-goruntusu.png";
        StreamResource resource = new StreamResource(fileName,
                () -> new ByteArrayInputStream(request.getScreenshotData()));

        Image image = new Image(resource, "Ekran görüntüsü");
        image.setWidthFull();
        image.getStyle()
                .set("max-height", "70vh")
                .set("object-fit", "contain")
                .set("border-radius", "8px");

        Button closeBtn = new Button(getTranslation("requests.btn.close"), e -> dialog.close());

        dialog.add(image);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private Component createStatusFilterHeader(Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        comboBox.setPlaceholder(getTranslation("requests.filter.statusPlaceholder"));
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder(getTranslation("requests.filter.startDate"));
        startPicker.setClearButtonVisible(true);
        startPicker.setWidthFull();

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder(getTranslation("requests.filter.endDate"));
        endPicker.setClearButtonVisible(true);
        endPicker.setWidthFull();

        startPicker.addValueChangeListener(e -> {
            endPicker.setMin(e.getValue());
            requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });

        endPicker.addValueChangeListener(e -> {
            startPicker.setMax(e.getValue());
            requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        VerticalLayout layout = new VerticalLayout(startPicker, endPicker);
        layout.setWidthFull();
        layout.setPadding(false);
        layout.setSpacing(true);
        return layout;
    }

    private void configureCloseDialog() {
        closeDialog.setHeaderTitle(getTranslation("helpdesk.triage.dialog.closeTitle"));
        closeReason.setLabel(getTranslation("helpdesk.triage.dialog.closeReasonLabel"));
        closeReason.setWidthFull();

        Button confirmCloseBtn = new Button(getTranslation("helpdesk.triage.dialog.closeConfirmBtn"), event -> {
            if (selectedRequest != null) {
                selectedRequest.setStatus("KAPATILDI");
                requestRepository.save(selectedRequest);

                String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
                systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + selectedRequest.getRequestId() + " olan talebi kapattı. Gerekçe: " + closeReason.getValue());

                if (selectedRequest.getCustomer() != null) {
                    notificationService.notifyUser(selectedRequest.getCustomer().getUserId(), getTranslation("helpdesk.triage.notif.requestClosedTitle"), getTranslation("helpdesk.triage.notif.descPrefix") + ": " + closeReason.getValue());
                }
                Notification.show(getTranslation("helpdesk.triage.notif.closed"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                closeDialog.close();
                closeReason.clear();
                selectedRequest = null;
                refreshGrid();
            }
        });
        confirmCloseBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button(getTranslation("requests.btn.cancel"), e -> closeDialog.close());

        closeDialog.getFooter().add(confirmCloseBtn, cancelBtn);
        closeDialog.add(closeReason);
    }

    private void forwardToPo(RequestEntity request) {
        request.setStatus("INCELEMEDE");
        requestRepository.save(request);

        String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
        systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi PO'ya sevk etti.");

        if (request.getCustomer() != null) {
            notificationService.notifyUser(request.getCustomer().getUserId(), getTranslation("helpdesk.triage.notif.underReviewTitle"), getTranslation("helpdesk.triage.notif.underReviewDesc"));
        }
        Notification.show(getTranslation("helpdesk.triage.notif.forwarded"), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }

    private void refreshGrid() {
        dataView = grid.setItems(requestRepository.findAll());
        requestFilter.setDataView(dataView); 
    }

    private static class RequestFilter {
        private GridListDataView<RequestEntity> dataView;
        private String searchTerm = "";
        private String status = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        
        private Integer assignedUserIdFilter = null;

        public void setDataView(GridListDataView<RequestEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setStatus(String status) {
            this.status = status != null ? status : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
            if (dataView != null) dataView.refreshAll();
        }

        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
            if (dataView != null) dataView.refreshAll();
        }

        public void setAssignedUserIdFilter(Integer assignedUserIdFilter) {
            this.assignedUserIdFilter = assignedUserIdFilter;
            if (dataView != null) dataView.refreshAll();
        }

        public boolean test(RequestEntity request) {
            boolean matchesSearch = true;
            if (!searchTerm.isEmpty()) {
                boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
                boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
                boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
                matchesSearch = inTitle || inDesc || inId;
            }

            boolean matchesStatus = status.isEmpty() ||
                    (request.getStatus() != null && request.getStatus().equalsIgnoreCase(status));

            boolean matchesDate = true;
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) matchesDate = false;
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) matchesDate = false;
            }

            boolean matchesAssignedUser = true;
            if (assignedUserIdFilter != null) {
                matchesAssignedUser = request.getAssignedUser() != null &&
                                      request.getAssignedUser().getUserId().equals(assignedUserIdFilter);
            }

            return matchesSearch && matchesStatus && matchesDate && matchesAssignedUser;
        }
    }
}