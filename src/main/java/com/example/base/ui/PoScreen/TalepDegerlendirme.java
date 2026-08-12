package com.example.base.ui.PoScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.PrioritizationEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.PrioritizationRepository;
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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker; 
import com.vaadin.flow.component.datepicker.DatePickerVariant;
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

@Route(value = "talep-degerlendir", layout = MainLayout.class)
@RolesAllowed({"PO", "GODPANEL"})
public class TalepDegerlendirme extends VerticalLayout implements HasDynamicTitle {

    private final RequestService requestService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final PrioritizationRepository prioritizationRepository;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;
    private final WorkflowRepository workflowRepository; 
    
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;

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
    private UserEntity currentUser;

    public TalepDegerlendirme(RequestService requestService, NotificationService notificationService,
                               ChatService chatService, UserRepository userRepository,
                               RequestRepository requestRepository, 
                               PrioritizationRepository prioritizationRepository,
                               SystemLogService systemLogService,
                               SettingsService settingsService,
                               WorkflowRepository workflowRepository,
                               InternalCommentService internalCommentService,
                               TeamChatBroadcaster teamChatBroadcaster) { 
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
        this.workflowRepository = workflowRepository; 
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        boolean isGod = currentUser != null && currentUser.getRole() != null && "GODPANEL".equals(currentUser.getRole().name());
        requestFilter.setGodPanel(isGod);

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

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.getStyle().set("margin-bottom", "16px").set("flex-shrink", "0");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        H2 heading = new H2(getTranslation("po.eval.heading"));
        heading.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-header-text-color)");
        Paragraph subtitle = new Paragraph(getTranslation("po.eval.subtitle"));
        subtitle.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        textLayout.add(heading, subtitle);

        Tab tabMine = new Tab(getTranslation("helpdesk.triage.tab.assignedToMe", "Bana Atanan Görevler"));
        Tab tabAll = new Tab(getTranslation("helpdesk.triage.tab.allPool", "Tüm Havuz"));
        Tabs tabs = new Tabs(tabMine, tabAll);
        
        tabs.getStyle().set("flex-shrink", "0");

        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(tabMine) && currentUser != null) {
                requestFilter.setAssignedUserIdFilter(currentUser.getUserId());
            } else {
                requestFilter.setAssignedUserIdFilter(null);
            }
        });

        headerLayout.add(textLayout, tabs);

        grid.setWidthFull();
        grid.getStyle()
                .set("flex-grow", "1")
                .set("background-color", "#ffffff")
                .set("border-radius", "12px");

        configureGrid();
        configureComboBoxes();
        configureCloseDialog();

        if (currentUser != null) {
            requestFilter.setAssignedUserIdFilter(currentUser.getUserId());
        }

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

        mainContainer.add(headerLayout, grid);
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
            if ("NEW".equals(request.getStatus()) || "DESTEK_KONTROL".equals(request.getStatus())) return "Destek";
            if ("INCELEMEDE".equals(request.getStatus()) || "PO_KONTROL".equals(request.getStatus())) return "PO";
            if ("ONAYLANDI".equals(request.getStatus()) || "İş Akışına Dönüştü".equals(request.getStatus())) return "Yazılım";
            return request.getStatus();
        }).setHeader("Sorumlu").setWidth("95px").setFlexGrow(0);

        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle).setHeader("Başlık").setFlexGrow(1);
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription).setHeader("Detay").setFlexGrow(2);
        
        Grid.Column<RequestEntity> effortCol = grid.addColumn(request -> {
            try {
                WorkflowEntity workflow = workflowRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
                if (workflow != null && workflow.getActualEffortHours() != null) {
                    return workflow.getActualEffortHours() + " S";
                }
            } catch (Exception e) {}
            return "-";
        }).setHeader("Efor").setWidth("75px").setFlexGrow(0);
        
        Grid.Column<RequestEntity> dateCol = grid.addColumn(request -> 
            request.getCreatedAt() != null ? request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) : "-"
        ).setHeader("Tarih").setWidth("120px").setFlexGrow(0);

        Grid.Column<RequestEntity> screenshotCol = grid.addComponentColumn(this::createScreenshotButton).setHeader("Görsel").setWidth("80px").setFlexGrow(0);
        
        Grid.Column<RequestEntity> scoreCol = grid.addColumn(request -> requestService.getRequestPriority(request.getRequestId()))
                .setHeader("Puan").setWidth("85px").setFlexGrow(0);

        Grid.Column<RequestEntity> evalCol = grid.addComponentColumn(request -> {
            if ("PO_KONTROL".equals(request.getStatus())) {
                Button qaBtn = new Button("Kontrol", VaadinIcon.CHECK_SQUARE_O.create());
                qaBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                qaBtn.addClickListener(e -> openQaDialog(request));
                return qaBtn;
            }

            Button evalBtn = new Button("Aksiyon", VaadinIcon.SLIDERS.create());
            evalBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            evalBtn.addClickListener(e -> {
                selectedRequest = request;
                try {
                    PrioritizationEntity p = prioritizationRepository.findByRequest_RequestId(request.getRequestId()).orElse(null);
                    if (p != null) {
                        if (p.getImpact() != null) impact.setValue(getTranslation("po.eval.impact." + p.getImpact()));
                        else impact.clear();

                        if (p.getUrgency() != null) urgency.setValue(getTranslation("po.eval.urgency." + p.getUrgency()));
                        else urgency.clear();

                        if (p.getEffort() != null) effort.setValue(getTranslation("po.eval.effort." + p.getEffort()));
                        else effort.clear();

                        securityOverride.setValue(p.getIsSecurityOverride() != null && p.getIsSecurityOverride() == 1);
                    } else {
                        impact.clear();
                        urgency.clear();
                        effort.clear();
                        securityOverride.setValue(false);
                    }
                } catch (Exception ex) {
                    impact.clear();
                    urgency.clear();
                    effort.clear();
                    securityOverride.setValue(false);
                }
                secim.open();
            });
            return evalBtn;
        }).setHeader("İşlem").setWidth("115px").setFlexGrow(0);

        Grid.Column<RequestEntity> chatCol = grid.addComponentColumn(request -> {
            Button chatButton = new Button(VaadinIcon.CHAT.create());
            chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            chatButton.getElement().setProperty("title", getTranslation("po.eval.tooltip.chat"));

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

            chatButton.addClickListener(e -> e.getSource().getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId())));
            return container;
        }).setHeader("Sohbet").setWidth("75px").setFlexGrow(0);

        Grid.Column<RequestEntity> historyCol = grid.addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create());
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            historyBtn.getElement().setProperty("title", getTranslation("po.eval.tooltip.history"));
            historyBtn.addClickListener(e -> openHistoryDialog(request));
            return historyBtn;
        }).setHeader("Geçmiş").setWidth("80px").setFlexGrow(0);

        Grid.Column<RequestEntity> ratingCol = grid.addComponentColumn(this::createRatingColumn).setHeader("Sonuç").setWidth("80px").setFlexGrow(0);
        Grid.Column<RequestEntity> slaCol = grid.addComponentColumn(this::createSlaBadge).setHeader("SLA").setWidth("105px").setFlexGrow(0);

        grid.addItemDoubleClickListener(event -> {
            RequestEntity request = event.getItem();
            if (request != null) {
                openRequestDetailDialog(request);
            }
        });

        TextField searchField = new TextField();
        searchField.setPlaceholder("Ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> requestFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(assigneeCol).setComponent(createComboBoxFilterHeader("Filtre", requestFilter::setAssignee));
        headerRow.getCell(titleCol).setComponent(searchField);
        headerRow.getCell(descCol).setComponent(new Span());
        headerRow.getCell(effortCol).setComponent(new Span()); 
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));
        headerRow.getCell(screenshotCol).setComponent(new Span());
        headerRow.getCell(scoreCol).setComponent(new Span());
        headerRow.getCell(evalCol).setComponent(new Span());
        headerRow.getCell(chatCol).setComponent(new Span());
        headerRow.getCell(historyCol).setComponent(new Span());
        headerRow.getCell(ratingCol).setComponent(new Span());
        headerRow.getCell(slaCol).setComponent(new Span());
    }

    private void openQaDialog(RequestEntity request) {
        Dialog qaDialog = new Dialog();
        qaDialog.setHeaderTitle("Yazılım Test & Onay");
        
        TextArea feedbackArea = new TextArea("Geri Bildirim / Not");
        feedbackArea.setWidthFull();
        feedbackArea.setPlaceholder("Test sonucuna göre notunuzu buraya yazın...");

        Button approveBtn = new Button("Onayla (Destek Ekibine Aktar)", e -> {
            request.setStatus("DESTEK_KONTROL");
            requestRepository.save(request);
            
            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() + " olan talebin yazılım testini ONAYLADI ve Destek ekibine aktardı. Not: " + feedbackArea.getValue());
            
            Notification.show("Talep test onayı alarak Destek Ekibine aktarıldı.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            qaDialog.close();
            refreshGrid();
        });
        approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        Button rejectBtn = new Button("Reddet (Yazılımcıya Geri Gönder)", e -> {
            request.setStatus("İş Akışına Dönüştü"); 
            requestRepository.save(request);
            
            String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
            systemLogService.log("PO (" + poEmail + "), ID: " + request.getRequestId() + " olan talebin yazılım testini REDDETTİ. Gerekçe: " + feedbackArea.getValue());
            
            Notification.show("Talep reddedilerek düzeltilmesi için Yazılım Ekibine geri gönderildi.", 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            qaDialog.close();
            refreshGrid();
        });
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("İptal", e -> qaDialog.close());

        qaDialog.add(new Paragraph("Yazılım ekibi bu görevi tamamladığını belirtti. Lütfen test edip onay durumunu seçin."), feedbackArea);
        qaDialog.getFooter().add(approveBtn, rejectBtn, cancelBtn);
        qaDialog.open();
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
        descArea.setMinHeight("150px");

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

    private Badge createSlaBadge(RequestEntity request) {
        Badge badge;
        if ("KAPATILDI".equals(request.getStatus())) {
            badge = new Badge("TAMAM");
            badge.addThemeVariants(BadgeVariant.CONTRAST); 
        } else if (request.getCreatedAt() == null) {
            badge = new Badge("-");
        } else {
            long hoursElapsed = ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());
            long slaLimitHours = settingsService.getSlaLimitHours();
            long warningLimitHours = (long) (slaLimitHours * settingsService.getSlaWarningPercent());

            if (hoursElapsed >= slaLimitHours) {
                badge = new Badge("İHLAL");
                badge.addThemeVariants(BadgeVariant.ERROR);
                badge.getElement().setProperty("title", getTranslation("requests.sla.violatedTitle"));
            } else if (hoursElapsed >= warningLimitHours) {
                badge = new Badge("UYARI");
                badge.addThemeVariants(BadgeVariant.WARNING);
                badge.getElement().setProperty("title", getTranslation("requests.sla.warningTitle"));
            } else {
                badge = new Badge("NORMAL");
                badge.addThemeVariants(BadgeVariant.SUCCESS);
            }
        }
        
        badge.getStyle().set("min-width", "85px").set("justify-content", "center").set("white-space", "nowrap");
        return badge;
    }

    private Component createRatingColumn(RequestEntity request) {
        if (request.getSatisfactionScore() != null) {
            Span pointBadge = new Span("⭐ " + request.getSatisfactionScore());
            pointBadge.getElement().getThemeList().add("badge success");
            pointBadge.getStyle().set("font-weight", "bold");
            
            if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                pointBadge.getElement().setProperty("title", getTranslation("helpdesk.triage.commentPrefix") + ": " + request.getSatisfactionComment());
                pointBadge.getStyle().set("cursor", "help");
            }
            return pointBadge;
        } else if ("KAPATILDI".equals(request.getStatus())) {
            return new Span("-");
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

        HorizontalLayout dateLayout = new HorizontalLayout(startPicker, endPicker);
        dateLayout.setPadding(false);
        dateLayout.setSpacing(false);
        dateLayout.getStyle().set("gap", "2px");
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

                String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
                
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
        Button cancelBtn = new Button(getTranslation("requests.btn.cancel"), e -> {
            closeDialog.close();
        });

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

    private int parseComboValue(String value) {
        if (value == null || value.isEmpty()) return 5;
        try {
            String numStr = value.split(" ")[0];
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return 5;
        }
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
                int urgencyPuan = parseComboValue(urgency.getValue());
                int impactPuan = parseComboValue(impact.getValue());
                int effortPuan = parseComboValue(effort.getValue());

                requestService.prioritizeRequest(selectedRequest.getRequestId(), urgencyPuan, impactPuan, effortPuan, secOverride);

                String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
                
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
                Notification.show(getTranslation("po.eval.error.alreadyTransferred"), 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            
            try {
                int urgencyPuan = urgency.getValue() != null ? parseComboValue(urgency.getValue()) : 5;
                int impactPuan = impact.getValue() != null ? parseComboValue(impact.getValue()) : 5;
                int effortPuan = effort.getValue() != null ? parseComboValue(effort.getValue()) : 1;
                boolean secOverride = securityOverride.getValue();

                int score = requestService.calculateScore(urgencyPuan, impactPuan, effortPuan, secOverride);
                int threshold = settingsService.getPoAutoApprovalThreshold();

                if (score < threshold && !secOverride) {
                    Notification.show("HATA: Bu talebin puanı (" + score + "), Admin tarafından belirlenen eşik değerinin (" + threshold + ") altında! Yazılım ekibine aktarılamaz.", 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return; 
                }
                
                requestService.prioritizeRequest(selectedRequest.getRequestId(), urgencyPuan, impactPuan, effortPuan, secOverride);
                requestService.goreveDonustur(selectedRequest);

                String poEmail = (currentUser != null) ? currentUser.getEmail() : "";
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
        private String assignee = "";
        private String searchTerm = "";
        private LocalDateTime startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
        private LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
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

        public void setAssignedUserIdFilter(Integer assignedUserIdFilter) {
            this.assignedUserIdFilter = assignedUserIdFilter;
            if (dataView != null) dataView.refreshAll();
        }

        public boolean test(RequestEntity request) {
            String mappedAssignee = request.getStatus();
            if ("NEW".equals(request.getStatus()) || "DESTEK_KONTROL".equals(request.getStatus())) mappedAssignee = "Destek Ekibi";
            else if ("INCELEMEDE".equals(request.getStatus()) || "PO_KONTROL".equals(request.getStatus())) mappedAssignee = "Ürün Yönetimi";
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

            boolean matchesAssignedUser = true;
            if (assignedUserIdFilter != null) {
                boolean isAssignedToMe = request.getAssignedUser() != null && request.getAssignedUser().getUserId().equals(assignedUserIdFilter);
                boolean isAssignedToPO = isGodPanel && request.getAssignedUser() != null && "PO".equals(request.getAssignedUser().getRole().name());
                boolean isInReviewPool = request.getAssignedUser() == null && ("INCELEMEDE".equals(request.getStatus()) || "PO_KONTROL".equals(request.getStatus()));
                
                matchesAssignedUser = isAssignedToMe || isAssignedToPO || isInReviewPool;
            }

            return matchesAssignee && matchesSearch && matchesDate && matchesAssignedUser;
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty() || 
                (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }
}