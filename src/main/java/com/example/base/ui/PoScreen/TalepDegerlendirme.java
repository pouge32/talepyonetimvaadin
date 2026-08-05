package com.example.base.ui.PoScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker; 
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-degerlendir", layout = MainLayout.class)
@RolesAllowed("PO")
public class TalepDegerlendirme extends VerticalLayout implements HasDynamicTitle {

    private final RequestService requestService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;

    private Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    private GridListDataView<RequestEntity> dataView;
    private final RequestFilter requestFilter = new RequestFilter();

    private final ComboBox<String> urgency = new ComboBox<>();
    private final ComboBox<String> impact = new ComboBox<>();
    private final ComboBox<String> effort = new ComboBox<>(); 
    private final Checkbox securityOverride = new Checkbox(); 

    private final Button scoreButton = new Button();
    private final Button aktar = new Button();
    private final Button kapatBtn = new Button();
    private final Button iptal = new Button();
    
    private final Dialog secim = new Dialog();
    private final Dialog closeDialog = new Dialog();
    private final TextArea closeReason = new TextArea();

    private RequestEntity selectedRequest;

    public TalepDegerlendirme(RequestService requestService, NotificationService notificationService,
                             ChatService chatService, UserRepository userRepository,
                             RequestRepository requestRepository, SystemLogService systemLogService,
                             SettingsService settingsService) {
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;

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
                .set("padding", "24px")
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("display", "flex")
                .set("flex-direction", "column");

        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("margin-bottom", "16px").set("flex-shrink", "0");
        H2 heading = new H2(getTranslation("po.eval.heading"));
        heading.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-header-text-color)");
        Paragraph subtitle = new Paragraph(getTranslation("po.eval.subtitle"));
        subtitle.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        header.add(heading, subtitle);

        grid.setWidthFull();
        grid.getStyle()
                .set("flex-grow", "1")
                .set("background-color", "#ffffff")
                .set("border-radius", "12px");

        configureGrid();
        configureComboBoxes();
        configureCloseDialog();

        scoreButton.setText(getTranslation("po.eval.btn.score"));
        scoreButton.addClickListener(event -> evaluateRequest());
        
        aktar.setText(getTranslation("po.eval.btn.transfer"));
        aktar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        aktar.getElement().setProperty("title", getTranslation("po.eval.tooltip.transfer"));
        aktar.addClickListener(event -> convertToWorkflow());
        
        kapatBtn.setText(getTranslation("po.eval.btn.closeReject"));
        kapatBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        kapatBtn.addClickListener(event -> {
            secim.close();
            closeDialog.open();
        });

        iptal.setText(getTranslation("po.eval.btn.cancel"));
        iptal.addClickListener(event -> cancel());

        secim.setHeaderTitle(getTranslation("po.eval.dialog.title"));
        secim.getFooter().add(scoreButton, aktar, kapatBtn, iptal);
        
        securityOverride.setLabel(getTranslation("po.eval.securityOverride"));
        securityOverride.getStyle().set("margin-top", "15px").set("font-weight", "bold").set("color", "var(--lumo-error-text-color)");
        
        impact.setLabel(getTranslation("po.eval.impact"));
        urgency.setLabel(getTranslation("po.eval.urgency"));
        effort.setLabel(getTranslation("po.eval.effort"));

        FormLayout formLayout = new FormLayout(impact, urgency, effort);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        secim.add(formLayout, securityOverride);
        secim.setCloseOnOutsideClick(false);
        secim.setWidth("450px");

        mainContainer.add(header, grid);
        add(mainContainer);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("po.eval.pageTitle");
    }

    private void cancel() {
        secim.close();
        selectedRequest = null;
    }

    private void configureGrid() {
        Grid.Column<RequestEntity> assigneeCol = grid.addColumn(request -> {
            if ("NEW".equals(request.getStatus())) return getTranslation("po.eval.team.support");
            if ("INCELEMEDE".equals(request.getStatus())) return getTranslation("po.eval.team.po");
            if ("ONAYLANDI".equals(request.getStatus()) || "İş Akışına Dönüştü".equals(request.getStatus())) return getTranslation("po.eval.team.software");
            return request.getStatus();
        }).setHeader(getTranslation("po.eval.grid.assignee")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle).setHeader(getTranslation("po.eval.grid.title")).setFlexGrow(1);
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription).setHeader(getTranslation("po.eval.grid.desc")).setFlexGrow(2);
        
        Grid.Column<RequestEntity> dateCol = grid.addColumn(request -> 
            request.getCreatedAt() != null ? request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-"
        ).setHeader(getTranslation("po.eval.grid.date")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(this::createScreenshotButton).setHeader(getTranslation("po.eval.grid.screenshot")).setAutoWidth(true).setFlexGrow(0);
        
        grid.addColumn(request -> requestService.getRequestPriority(request.getRequestId()))
                .setHeader(getTranslation("po.eval.grid.score")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            Button chatButton = new Button(VaadinIcon.CHAT.create());
            chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            chatButton.getElement().setProperty("title", getTranslation("po.eval.tooltip.chat"));

            Div container = new Div(chatButton);
            container.getStyle().set("position", "relative").set("display", "inline-block");

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = (auth != null) ? auth.getName() : "";
            UserEntity currentUser = userRepository.findByEmail(email).orElse(null);

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

            chatButton.addClickListener(e -> e.getSource().getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId())));
            return container;
        }).setHeader(getTranslation("po.eval.grid.chat")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create());
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            historyBtn.getElement().setProperty("title", getTranslation("po.eval.tooltip.history"));
            historyBtn.addClickListener(e -> openHistoryDialog(request));
            return historyBtn;
        }).setHeader(getTranslation("po.eval.grid.history")).setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(this::createRatingColumn).setHeader(getTranslation("po.eval.grid.rating")).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::createSlaBadge).setHeader(getTranslation("po.eval.grid.sla")).setAutoWidth(true).setFlexGrow(0);

        grid.asSingleSelect().addValueChangeListener(event -> {
            selectedRequest = event.getValue();
            if (selectedRequest != null) {
                secim.open();
            }
        });

        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("po.eval.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> requestFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(assigneeCol).setComponent(createComboBoxFilterHeader(getTranslation("po.eval.filter.assigneePlaceholder"), requestFilter::setAssignee));
        headerRow.getCell(titleCol).setComponent(searchField);
        headerRow.getCell(descCol).setComponent(new Span());
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));
    }

    private Badge createSlaBadge(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Badge closedBadge = new Badge(getTranslation("requests.sla.completed"));
            closedBadge.addThemeVariants(BadgeVariant.CONTRAST); 
            return closedBadge;
        }

        if(request.getCreatedAt() == null) return new Badge("-");

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
            return new Span(getTranslation("po.eval.unrated"));
        }
        return new Span("-");
    }

    private void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle(getTranslation("po.eval.history.title") + " (#" + request.getRequestId() + ")");
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());

            if (logs == null || logs.isEmpty()) {
                layout.add(new Span(getTranslation("po.eval.history.empty")));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.getStyle()
                            .set("border-left", "3px solid var(--lumo-primary-color)")
                            .set("padding-left", "15px")
                            .set("margin-bottom", "15px");

                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : getTranslation("po.eval.history.unknownDate");
                    Span dateSpan = new Span(dateStr);
                    dateSpan.getStyle()
                            .set("font-size", "0.85em")
                            .set("color", "var(--lumo-secondary-text-color)")
                            .set("display", "block")
                            .set("font-weight", "bold");

                    Span actionSpan = new Span(log.getAction());
                    actionSpan.getStyle()
                            .set("display", "block")
                            .set("margin-top", "4px");

                    stepItem.add(dateSpan, actionSpan);
                    layout.add(stepItem);
                }
            }
        } catch (Exception e) {
            layout.add(new Span(getTranslation("po.eval.history.error") + ": " + e.getMessage()));
        }

        historyDialog.add(layout);
        Button closeBtn = new Button(getTranslation("requests.btn.close"), e -> historyDialog.close());
        historyDialog.getFooter().add(closeBtn);
        
        historyDialog.open();
    }

    private Component createScreenshotButton(RequestEntity request) {
        boolean hasScreenshot = request.getScreenshotData() != null && request.getScreenshotData().length > 0;

        Button button = new Button(VaadinIcon.PICTURE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        if (!hasScreenshot) {
            button.setEnabled(false);
            button.getElement().setProperty("title", getTranslation("po.eval.tooltip.noImage"));
            return button;
        }

        button.getElement().setProperty("title", getTranslation("po.eval.tooltip.openImage"));
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

    private Component createComboBoxFilterHeader(String placeholder, Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(
            getTranslation("po.eval.team.support"), 
            getTranslation("po.eval.team.po"), 
            getTranslation("po.eval.team.software"), 
            getTranslation("requests.status.closed")
        );
        comboBox.setPlaceholder(placeholder);
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false);
        dateLayout.setSpacing(false);

        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder(getTranslation("requests.filter.startDate"));
        startPicker.setClearButtonVisible(true);
        startPicker.setWidth("110px");
        startPicker.getStyle().set("margin-bottom", "4px");
        startPicker.addValueChangeListener(e -> {
            requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder(getTranslation("requests.filter.endDate"));
        endPicker.setClearButtonVisible(true);
        endPicker.setWidth("110px");
        endPicker.addValueChangeListener(e -> {
            requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        dateLayout.add(startPicker, endPicker);
        return dateLayout;
    }

    private void configureCloseDialog() {
        closeDialog.setHeaderTitle(getTranslation("helpdesk.triage.dialog.closeTitle"));
        closeReason.setLabel(getTranslation("helpdesk.triage.dialog.closeReasonLabel"));
        closeReason.setWidthFull();

        Button confirmCloseBtn = new Button(getTranslation("helpdesk.triage.dialog.closeConfirmBtn"), event -> {
            if (selectedRequest != null) {
                selectedRequest.setStatus("KAPATILDI"); 
                requestRepository.save(selectedRequest);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String poEmail = (auth != null) ? auth.getName() : "";
                
                systemLogService.log("PO (" + poEmail + "), ID: " + selectedRequest.getRequestId() + " olan talebi kapattı. Gerekçe: " + closeReason.getValue());

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

    private void configureComboBoxes() {
        impact.setItems(
                getTranslation("po.eval.impact.1"),
                getTranslation("po.eval.impact.2"),
                getTranslation("po.eval.impact.3"),
                getTranslation("po.eval.impact.4"),
                getTranslation("po.eval.impact.5")
        );

        urgency.setItems(
                getTranslation("po.eval.urgency.1"),
                getTranslation("po.eval.urgency.2"),
                getTranslation("po.eval.urgency.3"),
                getTranslation("po.eval.urgency.4"),
                getTranslation("po.eval.urgency.5")
        );

        effort.setItems(
                getTranslation("po.eval.effort.1"),
                getTranslation("po.eval.effort.2"),
                getTranslation("po.eval.effort.3"),
                getTranslation("po.eval.effort.4"),
                getTranslation("po.eval.effort.5")
        );
    }

    private void evaluateRequest() {
        if (selectedRequest != null) {
            boolean secOverride = securityOverride.getValue();
            
            if (!secOverride && (urgency.getValue() == null || impact.getValue() == null || effort.getValue() == null)) {
                Notification error = Notification.show(getTranslation("po.eval.error.selectFields"), 3000, Notification.Position.MIDDLE);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                int urgencyPuan = urgency.getValue() != null ? Character.getNumericValue(urgency.getValue().charAt(0)) : 5;
                int impactPuan = impact.getValue() != null ? Character.getNumericValue(impact.getValue().charAt(0)) : 5;
                int effortPuan = effort.getValue() != null ? Character.getNumericValue(effort.getValue().charAt(0)) : 1;

                requestService.prioritizeRequest(selectedRequest.getRequestId(), urgencyPuan, impactPuan, effortPuan, secOverride);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String poEmail = (auth != null) ? auth.getName() : "";
                
                String overrideText = secOverride ? " [KRİTİK GÜVENLİK/KESİNTİ]" : "";
                systemLogService.log("PO (" + poEmail + "), ID: " + selectedRequest.getRequestId() + 
                                     " olan talebi önceliklendirdi." + overrideText + " (Aciliyet: " + urgencyPuan + 
                                     ", Etki: " + impactPuan + ", Efor: " + effortPuan + ")");

                if (selectedRequest.getCustomer() != null) {
                    notificationService.notifyUser(selectedRequest.getCustomer().getUserId(), getTranslation("po.eval.notif.prioritizedTitle"), "'" + selectedRequest.getTitle() + "' " + getTranslation("po.eval.notif.prioritizedDesc"));
                }

                Notification.show(getTranslation("po.eval.notif.prioritizedSuccess"), 3000, Notification.Position.TOP_CENTER);

                urgency.clear();
                impact.clear();
                effort.clear();
                securityOverride.setValue(false);
                selectedRequest = null;
                secim.close();
                refreshGrid();

            } catch (Exception e) {
                Notification.show("Hata: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
            }
        } else {
            Notification.show(getTranslation("po.eval.error.selectRequest"), 3000, Notification.Position.MIDDLE);
        }
    }

    private void convertToWorkflow() {
        if (selectedRequest != null) {
            
            if ("İş Akışına Dönüştü".equals(selectedRequest.getStatus())) {
                Notification.show(getTranslation("po.eval.error.alreadyTransferred"), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            
            if (!"ONAYLANDI".equals(selectedRequest.getStatus())) {
                Notification.show(getTranslation("po.eval.error.lowScore"), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                requestService.goreveDonustur(selectedRequest);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String poEmail = (auth != null) ? auth.getName() : "";
                
                systemLogService.log("PO (" + poEmail + "), ID: " + selectedRequest.getRequestId() + " olan talebi yazılım ekibine/göreve dönüştürdü.");

                if (selectedRequest.getCustomer() != null) {
                    notificationService.notifyUser(selectedRequest.getCustomer().getUserId(), getTranslation("po.eval.notif.convertedTitle"), "'" + selectedRequest.getTitle() + "' " + getTranslation("po.eval.notif.convertedDesc"));
                }

                Notification success = Notification.show(getTranslation("po.eval.notif.convertedSuccess"), 3000, Notification.Position.TOP_CENTER);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                selectedRequest = null;
                secim.close();
                refreshGrid();

            } catch (Exception e) {
                Notification error = Notification.show("Hata: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            Notification.show(getTranslation("po.eval.error.selectRequest"), 3000, Notification.Position.MIDDLE);
        }
    }

    private void refreshGrid() {
        dataView = grid.setItems(requestRepository.findAll());
        requestFilter.setDataView(dataView); 
    }

    private static class RequestFilter {
        private GridListDataView<RequestEntity> dataView;
        private String assignee = "";
        private String searchTerm = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public void setDataView(GridListDataView<RequestEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setAssignee(String assignee) {
            this.assignee = assignee != null ? assignee : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
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

        public boolean test(RequestEntity request) {
            String mappedAssignee = request.getStatus();
            if ("NEW".equals(request.getStatus())) mappedAssignee = "Destek Ekibi";
            else if ("INCELEMEDE".equals(request.getStatus())) mappedAssignee = "Ürün Yönetimi";
            else if ("ONAYLANDI".equals(request.getStatus()) || "İş Akışına Dönüştü".equals(request.getStatus())) mappedAssignee = "Yazılım Ekibi";

            boolean matchesAssignee = matches(mappedAssignee, assignee);

            boolean matchesSearch = true;
            if (!searchTerm.isEmpty()) {
                boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
                boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
                boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
                matchesSearch = inTitle || inDesc || inId;
            }

            boolean matchesDate = true;
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) matchesDate = false;
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) matchesDate = false;
            }
            return matchesAssignee && matchesSearch && matchesDate;
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty() || 
                (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }
}