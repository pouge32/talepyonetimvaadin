package com.example.base.ui.HelpDeskerScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.ChatService;
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.example.base.ui.Chat.TalepChat;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
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
@RolesAllowed({"HELPDESK", "GODPANEL"})
public class OnIncelemeView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;
    private final WorkflowRepository workflowRepository; 
    
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;
    
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    private GridListDataView<RequestEntity> dataView;
    private final RequestFilter requestFilter = new RequestFilter();
    
    private RequestEntity selectedRequest;
    private Dialog closeDialog = new Dialog();
    private TextArea closeReason = new TextArea();

    private UserEntity currentUser;

    public OnIncelemeView(RequestRepository requestRepository, RequestService requestService, NotificationService notificationService,
                          ChatService chatService, UserRepository userRepository,
                          SystemLogService systemLogService, SettingsService settingsService,
                          WorkflowRepository workflowRepository,
                          InternalCommentService internalCommentService, TeamChatBroadcaster teamChatBroadcaster) { 
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
        this.workflowRepository = workflowRepository; 
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        boolean isGod = currentUser != null && currentUser.getRole() != null && "GODPANEL".equals(currentUser.getRole().name());
        requestFilter.setGodPanel(isGod);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H3 title = new H3(getTranslation("helpdesk.triage.headerTitle"));
        title.getStyle().set("margin", "0");

        Tab tabMine = new Tab(getTranslation("helpdesk.triage.tab.assignedToMe"));
        Tab tabAll = new Tab(getTranslation("helpdesk.triage.tab.allPool"));
        Tabs tabs = new Tabs(tabMine, tabAll);
        
        tabs.getStyle().set("flex-shrink", "0");
        
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
        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle)
                .setHeader(getTranslation("helpdesk.triage.grid.title"))
                .setFlexGrow(1); 
                
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription)
                .setHeader(getTranslation("helpdesk.triage.grid.desc"))
                .setFlexGrow(2); 
        
        Grid.Column<RequestEntity> assignedCol = grid.addColumn(req -> {
            try {
                return req.getAssignedUser() != null ? req.getAssignedUser().getNameSurname() : getTranslation("helpdesk.triage.unassigned");
            } catch (Exception e) {
                return getTranslation("helpdesk.triage.unassigned");
            }
        }).setHeader("Uzman") 
          .setWidth("120px").setFlexGrow(0); 

        Grid.Column<RequestEntity> statusCol = grid.addComponentColumn(this::createStatusBadge)
                .setHeader(getTranslation("helpdesk.triage.grid.status"))
                .setWidth("140px").setFlexGrow(0); 
                
        Grid.Column<RequestEntity> effortCol = grid.addColumn(request -> {
            try {
                WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
                if (workflow != null && workflow.getActualEffortHours() != null) {
                    return workflow.getActualEffortHours() + " S"; 
                }
            } catch (Exception e) {}
            return "-";
        }).setHeader("Efor") 
          .setWidth("75px").setFlexGrow(0);
        
        Grid.Column<RequestEntity> dateCol = grid.addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) : "-")
                .setHeader("Tarih") 
                .setWidth("185px").setFlexGrow(0); 

        Grid.Column<RequestEntity> screenCol = grid.addComponentColumn(this::createScreenshotButton)
                .setHeader("Görsel") 
                .setWidth("80px").setFlexGrow(0);

        Grid.Column<RequestEntity> chatCol = grid.addComponentColumn(request -> {
            Button chatButton = new Button(VaadinIcon.CHAT.create());
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
        }).setHeader("Sohbet") 
          .setWidth("75px").setFlexGrow(0); 

        Grid.Column<RequestEntity> historyCol = grid.addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create());
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            historyBtn.getElement().setProperty("title", "Geçmişi Gör");
            historyBtn.addClickListener(e -> openHistoryDialog(request));
            return historyBtn;
        }).setHeader("Geçmiş")
          .setWidth("80px").setFlexGrow(0);

        Grid.Column<RequestEntity> ratingCol = grid.addComponentColumn(this::createRatingColumn)
                .setHeader("Puan") 
                .setWidth("75px").setFlexGrow(0);

        Grid.Column<RequestEntity> actionCol = grid.addComponentColumn(request -> {
            HorizontalLayout actLayout = new HorizontalLayout();
            actLayout.setSpacing(true);
            actLayout.setPadding(false);

            if ("NEW".equals(request.getStatus())) {
                Button closeBtn = new Button(VaadinIcon.CLOSE_CIRCLE.create());
                closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                closeBtn.getElement().setProperty("title", getTranslation("helpdesk.triage.btn.closeRequest"));
                closeBtn.addClickListener(e -> {
                    selectedRequest = request;
                    closeDialog.open();
                });

                Button sendToPoBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
                sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                sendToPoBtn.getElement().setProperty("title", getTranslation("helpdesk.triage.btn.forwardPo"));
                sendToPoBtn.addClickListener(e -> forwardToPo(request));

                actLayout.add(closeBtn, sendToPoBtn);
                
            } else if ("DESTEK_KONTROL".equals(request.getStatus())) {
                Button confirmBtn = new Button("Teyit", VaadinIcon.PHONE.create());
                confirmBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                confirmBtn.addClickListener(e -> openCustomerConfirmationDialog(request));
                actLayout.add(confirmBtn);
                
            } else {
                actLayout.add(new Span("-"));
            }

            return actLayout;
        }).setHeader("İşlem") 
          .setWidth("100px").setFlexGrow(0);

        Grid.Column<RequestEntity> slaCol = grid.addComponentColumn(this::createSlaBadge)
                .setHeader("SLA")
                .setWidth("105px").setFlexGrow(0);

        grid.addItemDoubleClickListener(event -> {
            RequestEntity request = event.getItem();
            if (request != null) {
                openRequestDetailDialog(request);
            }
        });

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
        headerRow.getCell(effortCol).setComponent(new Span()); 
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));
        headerRow.getCell(screenCol).setComponent(new Span());
        headerRow.getCell(chatCol).setComponent(new Span());
        headerRow.getCell(historyCol).setComponent(new Span()); 
        headerRow.getCell(ratingCol).setComponent(new Span());
        headerRow.getCell(actionCol).setComponent(new Span());
        headerRow.getCell(slaCol).setComponent(new Span());
    }

    private void openCustomerConfirmationDialog(RequestEntity request) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Müşteri Teyidi (QA Son Adım)");
        confirmDialog.setWidth("450px");

        TextArea noteArea = new TextArea("Müşteri Görüşme Notu");
        noteArea.setWidthFull();
        noteArea.setPlaceholder("Müşteri ile yapılan teyit görüşmesi detaylarını buraya yazın...");

        Button closeRequestBtn = new Button("Kapat (Sorun Çözüldü)", e -> {
            if (noteArea.getValue().trim().isEmpty()) {
                Notification.show("Lütfen görüşme notu giriniz.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            request.setStatus("KAPATILDI");
            requestRepository.save(request);

            String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
            systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi Müşteri Teyidi ile KAPATTI. Not: " + noteArea.getValue());

            if (request.getCustomer() != null) {
                notificationService.notifyUser(request.getCustomer().getUserId(), "Talep Çözüldü", "Talebiniz Destek Ekibi tarafından teyit edilerek başarıyla kapatılmıştır. Not: " + noteArea.getValue());
            }

            Notification.show("Talep müşteri teyidi ile başarıyla kapatıldı.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            confirmDialog.close();
            refreshGrid();
        });
        closeRequestBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        Button returnToPoBtn = new Button("PO'ya Geri Gönder (Çözülmedi)", e -> {
            if (noteArea.getValue().trim().isEmpty()) {
                Notification.show("Lütfen geri gönderme gerekçesi giriniz.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            request.setStatus("INCELEMEDE"); 
            requestRepository.save(request);

            String staffEmail = (currentUser != null) ? currentUser.getEmail() : "Bilinmiyor";
            systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi Müşteri Teyidi aşamasında ÇÖZÜLEMEDİĞİ için PO'ya GERİ GÖNDERDİ. Gerekçe: " + noteArea.getValue());

            Notification.show("Talep incelenmesi için yeniden Ürün Yöneticisi'ne (PO) gönderildi.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            confirmDialog.close();
            refreshGrid();
        });
        returnToPoBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("İptal", e -> confirmDialog.close());

        VerticalLayout dialogLayout = new VerticalLayout(new Paragraph("Yazılım süreci tamamlanan bu talep için lütfen müşteri ile iletişime geçip çözümün başarılı olup olmadığını teyit edin."), noteArea);
        dialogLayout.setPadding(false);

        confirmDialog.add(dialogLayout);
        confirmDialog.getFooter().add(closeRequestBtn, returnToPoBtn, cancelBtn);
        confirmDialog.open();
    }

    private void openRequestDetailDialog(RequestEntity request) {
        Dialog detailDialog = new Dialog();
        detailDialog.setHeaderTitle("Talep Detayı #" + request.getRequestId());
        detailDialog.setWidth("650px"); 
        detailDialog.setMaxHeight("85vh");

        Tabs tabs = new Tabs();
        Tab detayTab = new Tab("Talep Bilgileri");
        Tab yazismaTab = new Tab("İç Yazışma (Takım)");
        tabs.add(detayTab, yazismaTab);

        VerticalLayout detayLayout = new VerticalLayout();
        detayLayout.setPadding(false);
        detayLayout.setSpacing(true);

        Span titleSpan = new Span("Başlık: " + request.getTitle());
        titleSpan.getStyle().set("font-weight", "bold");

        TextArea descArea = new TextArea("Açıklama");
        descArea.setValue(request.getDescription() != null ? request.getDescription() : "Açıklama bulunmuyor.");
        descArea.setReadOnly(true);
        descArea.setWidthFull();
        descArea.setMinHeight("100px");

        detayLayout.add(titleSpan, descArea);

        try {
            WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
            if (workflow != null && workflow.getActualEffortHours() != null) {
                Span effortBadge = new Span("⏱️ Harcanan Efor: " + workflow.getActualEffortHours() + " Saat");
                effortBadge.getElement().getThemeList().add("badge success primary");
                effortBadge.getStyle().set("margin-top", "10px").set("font-size", "14px");
                detayLayout.add(effortBadge);
            }
        } catch (Exception ignored) {}

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());
            if (logs != null && !logs.isEmpty()) {
                detayLayout.add(new com.vaadin.flow.component.html.Hr());
                Span historyTitle = new Span("Talep Geçmişi");
                historyTitle.getStyle().set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
                detayLayout.add(historyTitle);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.getStyle()
                            .set("border-left", "3px solid var(--lumo-primary-color)")
                            .set("padding-left", "15px")
                            .set("margin-bottom", "10px");

                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "";
                    Span dateSpan = new Span(dateStr);
                    dateSpan.getStyle().set("font-size", "0.80em").set("color", "var(--lumo-secondary-text-color)").set("display", "block");

                    Span actionSpan = new Span(log.getAction());
                    actionSpan.getStyle().set("display", "block").set("font-size", "0.9em");

                    stepItem.add(dateSpan, actionSpan);
                    detayLayout.add(stepItem);
                }
            }
        } catch (Exception ignored) {}
        
        com.example.base.ui.Chat.InternalChatPanel chatPanel = 
            new com.example.base.ui.Chat.InternalChatPanel(internalCommentService, teamChatBroadcaster, request.getRequestId(), currentUser);
        chatPanel.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            detayLayout.setVisible(event.getSelectedTab().equals(detayTab));
            chatPanel.setVisible(event.getSelectedTab().equals(yazismaTab));
        });

        detailDialog.add(tabs, detayLayout, chatPanel);

        Button closeBtn = new Button("Kapat", e -> detailDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        detailDialog.getFooter().add(closeBtn);

        detailDialog.open();
    }

    private void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle("Talep Geçmişi (#" + request.getRequestId() + ")");
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());

            if (logs == null || logs.isEmpty()) {
                layout.add(new Span("Bu talep için henüz bir geçmiş bulunmuyor."));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.getStyle()
                            .set("border-left", "3px solid var(--lumo-primary-color)")
                            .set("padding-left", "15px")
                            .set("margin-bottom", "15px");

                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "-";
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
            layout.add(new Span("Geçmiş yüklenirken hata oluştu: " + e.getMessage()));
        }

        historyDialog.add(layout);
        Button closeBtn = new Button("Kapat", e -> historyDialog.close());
        historyDialog.getFooter().add(closeBtn);
        
        historyDialog.open();
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
        badge.getStyle().set("min-width", "80px").set("justify-content", "center").set("white-space", "nowrap");
        
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
        comboBox.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "DESTEK_KONTROL", "KAPATILDI");
        comboBox.setPlaceholder(getTranslation("requests.filter.statusPlaceholder"));
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false);
        dateLayout.setSpacing(false);
        dateLayout.getStyle().set("gap", "4px");

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
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("gap", "4px");
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
        
        userRepository.findAll().stream()
            .filter(u -> u.getRole() != null && u.getRole().name().equals("PO"))
            .findFirst()
            .ifPresent(request::setAssignedUser);

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
        List<RequestEntity> requests = requestService.getAllRequestsForGrid();
        
        requests.sort((r1, r2) -> {
            int score1 = (r1.getPrioritization() != null) ? r1.getPrioritization().getPriorityScore() : -1;
            int score2 = (r2.getPrioritization() != null) ? r2.getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1);
        });

        dataView = grid.setItems(requests);
        requestFilter.setDataView(dataView); 
    }

    private static class RequestFilter {
        private GridListDataView<RequestEntity> dataView;
        private String searchTerm = "";
        private String status = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        
        private Integer assignedUserIdFilter = null;
        
        private boolean isGodPanel = false;

        public void setGodPanel(boolean isGodPanel) {
            this.isGodPanel = isGodPanel;
            if (dataView != null) dataView.refreshAll();
        }

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
                boolean isAssignedToMe = request.getAssignedUser() != null && 
                                         request.getAssignedUser().getUserId().equals(assignedUserIdFilter);
                
                boolean isAssignedToHelpdesk = isGodPanel && request.getAssignedUser() != null && 
                                               "HELPDESK".equals(request.getAssignedUser().getRole().name());
                
                boolean isMyPoolTask = "NEW".equals(request.getStatus()) || "DESTEK_KONTROL".equals(request.getStatus());
                                               
                matchesAssignedUser = isAssignedToMe || isAssignedToHelpdesk || isMyPoolTask;
            }

            return matchesSearch && matchesStatus && matchesDate && matchesAssignedUser;
        }
    }
}