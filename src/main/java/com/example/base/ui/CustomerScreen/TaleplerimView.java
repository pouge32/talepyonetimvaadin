package com.example.base.ui.CustomerScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatService;
import com.example.base.service.RequestService;
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

@Route(value = "taleplerim", layout = MainLayout.class)
@RolesAllowed(value = "CUSTOMER")
public class TaleplerimView extends VerticalLayout implements HasDynamicTitle {

    private final RequestService requestService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final RequestRepository requestRepository;
    private final SettingsService settingsService;
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);

    public TaleplerimView(RequestService requestService, ChatService chatService, 
                          UserRepository userRepository, SystemLogService systemLogService,
                          RequestRepository requestRepository, SettingsService settingsService) {
        this.requestService = requestService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.requestRepository = requestRepository;
        this.settingsService = settingsService;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        systemLogService.log("Müşteri (" + email + ") taleplerim sayfasını görüntüledi.");

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                  .set("overflow", "hidden");

        configureGrid();

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "24px")
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-sizing", "border-box");

        grid.setWidthFull();
        grid.getStyle()
                .set("flex-grow", "1")
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "12px");

        mainContainer.add(buildPageHeader(), grid);
        add(mainContainer);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("requests.pageTitle");
    }

    private Div buildPageHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("margin-bottom", "16px").set("flex-shrink", "0");

        H2 heading = new H2(getTranslation("requests.heading"));
        heading.getStyle().set("margin", "0 0 2px 0").set("color", "var(--lumo-header-text-color)").set("font-size", "22px");

        Paragraph subtitle = new Paragraph(getTranslation("requests.subtitle"));
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        header.add(heading, subtitle);
        return header;
    }

    private void configureGrid() {
        Grid.Column<RequestEntity> idColumn = grid.addColumn(RequestEntity::getRequestId).setHeader(getTranslation("requests.grid.id")).setAutoWidth(true).setFlexGrow(0);
        
        Grid.Column<RequestEntity> titleColumn = grid.addColumn(RequestEntity::getTitle).setHeader(getTranslation("requests.grid.title")).setFlexGrow(2);
        Grid.Column<RequestEntity> descColumn = grid.addColumn(RequestEntity::getDescription).setHeader(getTranslation("requests.grid.desc")).setFlexGrow(2);
        
        Grid.Column<RequestEntity> screenshotColumn = grid.addComponentColumn(this::createScreenshotButton)
                .setHeader(getTranslation("requests.grid.screenshot")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<RequestEntity> dateColumn = grid.addColumn(request -> request.getCreatedAt() != null ? request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-")
                .setHeader(getTranslation("requests.grid.createdAt")).setAutoWidth(true).setFlexGrow(0);
                
        Grid.Column<RequestEntity> statusColumn = grid.addComponentColumn(this::createStatusBadge).setHeader(getTranslation("requests.grid.status")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<RequestEntity> actionsColumn = grid.addComponentColumn(request -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.getThemeList().add("spacing-s");
            actions.setAlignItems(FlexComponent.Alignment.CENTER);

            actions.add(createChatButton(request));

            if ("KAPATILDI".equals(request.getStatus())) {
                Button reopenBtn = new Button(getTranslation("requests.btn.reopen"), VaadinIcon.REFRESH.create());
                reopenBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                reopenBtn.getElement().setProperty("title", getTranslation("requests.tooltip.reopen"));
                reopenBtn.addClickListener(e -> openReopenDialog(request));
                actions.add(reopenBtn);
            }

            return actions;
        }).setHeader(getTranslation("requests.grid.actions")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<RequestEntity> ratingColumn = grid.addComponentColumn(this::createRatingColumn).setHeader(getTranslation("requests.grid.rating")).setAutoWidth(true).setFlexGrow(0);
        Grid.Column<RequestEntity> slaColumn = grid.addComponentColumn(this::createSlaBadge).setHeader(getTranslation("requests.grid.sla")).setAutoWidth(true).setFlexGrow(0);

        GridListDataView<RequestEntity> dataView = grid.setItems(requestService.getMyRequestsForCurrentUser());
        RequestFilter requestFilter = new RequestFilter(dataView);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        requestFilter.setStartDate(oneWeekAgo);
        requestFilter.setEndDate(now);

        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("requests.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> requestFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        
        headerRow.getCell(idColumn).setComponent(new Span());
        headerRow.getCell(titleColumn).setComponent(searchField);
        headerRow.getCell(descColumn).setComponent(new Span());
        headerRow.getCell(screenshotColumn).setComponent(new Span());
        headerRow.getCell(dateColumn).setComponent(createDateRangeFilterHeader(requestFilter));
        headerRow.getCell(statusColumn).setComponent(createStatusFilterHeader(requestFilter::setStatus));
        headerRow.getCell(actionsColumn).setComponent(new Span());
        headerRow.getCell(ratingColumn).setComponent(new Span());
        headerRow.getCell(slaColumn).setComponent(new Span());
    }

    private void openReopenDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("requests.dialog.reopenTitle") + " (#" + request.getRequestId() + ")");
        dialog.setWidth("500px");

        TextArea reasonArea = new TextArea(getTranslation("requests.dialog.reasonLabel"));
        reasonArea.setWidthFull();
        reasonArea.setPlaceholder(getTranslation("requests.dialog.reasonPlaceholder"));
        reasonArea.setRequired(true);

        Button confirmBtn = new Button(getTranslation("requests.btn.reopenConfirm"), e -> {
            if (reasonArea.isEmpty()) {
                Notification.show(getTranslation("requests.notification.reasonRequired"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                requestService.reopenRequest(request.getRequestId(), reasonArea.getValue());
                
                Notification.show(getTranslation("requests.notification.reopened"), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button(getTranslation("requests.btn.cancel"), e -> dialog.close());

        dialog.add(new Paragraph(getTranslation("requests.dialog.reopenDesc")), reasonArea);
        dialog.getFooter().add(confirmBtn, cancelBtn);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(requestService.getMyRequestsForCurrentUser());
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
                pointBadge.getStyle().set("font-weight", "bold");
                return pointBadge;
            } else {
                Button rateBtn = new Button(getTranslation("requests.btn.rate"), VaadinIcon.STAR.create());
                rateBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                rateBtn.addClickListener(e -> openRatingDialog(request));
                return rateBtn;
            }
        }
        return new Span("-");
    }

    private void openRatingDialog(RequestEntity request) {
        Dialog ratingDialog = new Dialog();
        ratingDialog.setHeaderTitle(getTranslation("requests.dialog.ratingTitle"));
        ratingDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();

        RadioButtonGroup<Integer> scoreGroup = new RadioButtonGroup<>();
        scoreGroup.setLabel(getTranslation("requests.dialog.scoreLabel"));
        scoreGroup.setItems(1, 2, 3, 4, 5);
        scoreGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        
        TextArea commentArea = new TextArea(getTranslation("requests.dialog.commentLabel"));
        commentArea.setPlaceholder(getTranslation("requests.dialog.commentPlaceholder"));
        commentArea.setWidthFull();

        layout.add(scoreGroup, commentArea);
        ratingDialog.add(layout);

        Button submitBtn = new Button(getTranslation("requests.btn.submitRating"), event -> {
            if (scoreGroup.getValue() == null) {
                Notification.show(getTranslation("requests.notification.scoreRequired"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            requestService.rateRequest(request.getRequestId(), scoreGroup.getValue(), commentArea.getValue());

            Notification.show(getTranslation("requests.notification.rated"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            ratingDialog.close();
            refreshGrid();
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button(getTranslation("requests.btn.close"), e -> ratingDialog.close());

        ratingDialog.getFooter().add(submitBtn, cancelBtn);
        ratingDialog.open();
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

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button(getTranslation("requests.btn.chat"), VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        com.vaadin.flow.component.html.Div container = new com.vaadin.flow.component.html.Div(chatButton);
        container.getStyle().set("position", "relative").set("display", "inline-block");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        
        userRepository.findByEmail(email).ifPresent(currentUser -> {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.getStyle()
                        .set("position", "absolute")
                        .set("top", "-5px")
                        .set("right", "-5px")
                        .set("padding", "2px 6px")
                        .set("font-size", "10px")
                        .set("font-weight", "bold");
                container.add(badge);
            }
        });

        chatButton.addClickListener(e -> {
            systemLogService.log("Müşteri (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine giriş yaptı.");
            e.getSource().getUI().ifPresent(ui ->
                    ui.navigate(TalepChat.class, request.getRequestId()));
        });

        return container;
    }

    private Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false);
        dateLayout.setSpacing(true);

        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder(getTranslation("requests.filter.startDate"));
        startPicker.setWidthFull();
        startPicker.setClearButtonVisible(true);
        startPicker.setValue(LocalDate.now().minusWeeks(1));
        startPicker.addValueChangeListener(e -> requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null));

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder(getTranslation("requests.filter.endDate"));
        endPicker.setWidthFull();
        endPicker.setClearButtonVisible(true);
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

    private static class RequestFilter {
        private final GridListDataView<RequestEntity> dataView;

        private String searchTerm = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String status = "";

        public RequestFilter(GridListDataView<RequestEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
            this.dataView.refreshAll();
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
            this.dataView.refreshAll();
        }

        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
            this.dataView.refreshAll();
        }

        public void setStatus(String status) {
            this.status = status != null ? status : "";
            this.dataView.refreshAll();
        }

        public boolean test(RequestEntity request) {
            boolean matchesSearch = true;
            if (!searchTerm.isEmpty()) {
                boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
                boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
                boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
                matchesSearch = inTitle || inDesc || inId;
            }

            boolean matchesDate = true;
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) {
                    matchesDate = false;
                }
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) {
                    matchesDate = false;
                }
            }

            String rawStatus = request.getStatus() != null ? request.getStatus() : "";
            boolean matchesStatus = status.isEmpty() || rawStatus.equalsIgnoreCase(status) || mapRawStatus(rawStatus).equalsIgnoreCase(status);

            return matchesSearch && matchesDate && matchesStatus;
        }

        private String mapRawStatus(String status) {
            return switch (status) {
                case "NEW" -> "Yeni";
                case "INCELEMEDE", "İncelemede" -> "İncelemede";
                case "ONAYLANDI", "İş Akışına Dönüştü" -> "İşleme Alındı";
                case "KAPATILDI" -> "KAPATILDI";
                default -> status;
            };
        }
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(statusLabel(status));
        String tooltipText = "";

        switch (status) {
            case "NEW":
                badge.addThemeVariants(BadgeVariant.CONTRAST);
                tooltipText = getTranslation("requests.tooltip.statusNew");
                break;
            case "INCELEMEDE":
            case "İncelemede":
                badge.addThemeVariants(BadgeVariant.WARNING);
                tooltipText = getTranslation("requests.tooltip.statusInReview");
                break;
            case "ONAYLANDI":
            case "İş Akışına Dönüştü":
                badge.addThemeVariants(BadgeVariant.SUCCESS);
                tooltipText = getTranslation("requests.tooltip.statusInProgress");
                break;
            case "KAPATILDI":
                badge.addThemeVariants(BadgeVariant.ERROR);
                tooltipText = getTranslation("requests.tooltip.statusClosed");
                break;
            default:
                badge.addThemeVariants(BadgeVariant.ERROR);
                tooltipText = getTranslation("requests.tooltip.statusDefault");
                break;
        }

        badge.getElement().setProperty("title", tooltipText);
        badge.getStyle().set("cursor", "help");

        return badge;
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