package com.example.base.ui.PoScreen.TalepDegerlendirme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-degerlendir", layout = MainLayout.class)
@RolesAllowed({"PO", "GODPANEL"})
@CssImport("./styles/po/talep-degerlendirme.css")
public class TalepDegerlendirmeView extends VerticalLayout implements HasDynamicTitle {

    private final RequestService requestService;
    private final ChatService chatService;
    private final SettingsService settingsService;
    private final WorkflowRepository workflowRepository; 
    
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    
    private final TalepDegerlendirmeFilter filterHelper;
    private final TalepActionDialogs actionDialogs;
    private final TalepViewDialogs viewDialogs;
    private final UserEntity currentUser;

    public TalepDegerlendirmeView(RequestService requestService, NotificationService notificationService,
                               ChatService chatService, UserRepository userRepository,
                               RequestRepository requestRepository, 
                               PrioritizationRepository prioritizationRepository,
                               SystemLogService systemLogService,
                               SettingsService settingsService,
                               WorkflowRepository workflowRepository,
                               InternalCommentService internalCommentService,
                               TeamChatBroadcaster teamChatBroadcaster) { 
        this.requestService = requestService;
        this.chatService = chatService;
        this.settingsService = settingsService;
        this.workflowRepository = workflowRepository; 

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("po-eval-layout");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        this.filterHelper = new TalepDegerlendirmeFilter(this);
        
        // Yeni aksiyon diyalogları
        this.actionDialogs = new TalepActionDialogs(this, currentUser, this::refreshGrid, 
            requestService, notificationService, requestRepository, prioritizationRepository, 
            systemLogService, settingsService);
            
        // Yeni görüntüleme diyalogları
        this.viewDialogs = new TalepViewDialogs(this, currentUser, 
            systemLogService, workflowRepository, internalCommentService, teamChatBroadcaster);

        boolean isGod = currentUser != null && currentUser.getRole() != null && "GODPANEL".equals(currentUser.getRole().name());
        filterHelper.setGodPanel(isGod);

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("po-eval-main-container");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.addClassName("po-eval-header-layout");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        H2 heading = new H2(getTranslation("po.eval.heading"));
        heading.addClassName("po-eval-heading");
        Paragraph subtitle = new Paragraph(getTranslation("po.eval.subtitle"));
        subtitle.addClassName("po-eval-subtitle");
        textLayout.add(heading, subtitle);

        Tab tabMine = new Tab(getTranslation("helpdesk.triage.tab.assignedToMe", "Bana Atanan Görevler"));
        Tab tabAll = new Tab(getTranslation("helpdesk.triage.tab.allPool", "Tüm Havuz"));
        Tabs tabs = new Tabs(tabMine, tabAll);
        tabs.addClassName("po-eval-tabs");

        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(tabMine) && currentUser != null) {
                filterHelper.setAssignedUserIdFilter(currentUser.getUserId());
            } else {
                filterHelper.setAssignedUserIdFilter(null);
            }
        });

        headerLayout.add(textLayout, tabs);

        grid.setWidthFull();
        grid.addClassName("po-eval-grid");

        configureGrid();

        if (currentUser != null) {
            filterHelper.setAssignedUserIdFilter(currentUser.getUserId());
        }

        mainContainer.add(headerLayout, grid);
        add(mainContainer);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("po.eval.pageTitle");
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
                if (workflow != null && workflow.getActualEffortHours() != null) return workflow.getActualEffortHours() + " S";
            } catch (Exception e) {}
            return "-";
        }).setHeader("Efor").setWidth("75px").setFlexGrow(0);
        
        Grid.Column<RequestEntity> dateCol = grid.addColumn(request -> 
            request.getCreatedAt() != null ? request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) : "-"
        ).setHeader("Tarih").setWidth("120px").setFlexGrow(0);

        Grid.Column<RequestEntity> screenshotCol = grid.addComponentColumn(request -> {
            Button button = new Button(VaadinIcon.PICTURE.create());
            button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            if (request.getScreenshotData() == null || request.getScreenshotData().length == 0) {
                button.setEnabled(false);
                button.getElement().setProperty("title", getTranslation("po.eval.tooltip.noImage"));
            } else {
                button.getElement().setProperty("title", getTranslation("po.eval.tooltip.openImage"));
                button.addClickListener(e -> viewDialogs.openScreenshotDialog(request));
            }
            return button;
        }).setHeader("Görsel").setWidth("80px").setFlexGrow(0);
        
        Grid.Column<RequestEntity> scoreCol = grid.addColumn(request -> requestService.getRequestPriority(request.getRequestId()))
                .setHeader("Puan").setWidth("85px").setFlexGrow(0);

        Grid.Column<RequestEntity> evalCol = grid.addComponentColumn(request -> {
            if ("PO_KONTROL".equals(request.getStatus())) {
                Button qaBtn = new Button("Kontrol", VaadinIcon.CHECK_SQUARE_O.create());
                qaBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                qaBtn.addClickListener(e -> actionDialogs.openQaDialog(request));
                return qaBtn;
            }
            Button evalBtn = new Button("Aksiyon", VaadinIcon.SLIDERS.create());
            evalBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            evalBtn.addClickListener(e -> actionDialogs.openEvaluationDialog(request));
            return evalBtn;
        }).setHeader("İşlem").setWidth("115px").setFlexGrow(0);

        Grid.Column<RequestEntity> chatCol = grid.addComponentColumn(request -> {
            Button chatButton = new Button(VaadinIcon.CHAT.create());
            chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            chatButton.getElement().setProperty("title", getTranslation("po.eval.tooltip.chat"));

            Div container = new Div(chatButton);
            container.addClassName("po-eval-chat-container");

            if (currentUser != null) {
                int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
                if (unreadCount > 0) {
                    Span badge = new Span(String.valueOf(unreadCount));
                    badge.getElement().getThemeList().add("badge error primary pill");
                    badge.addClassName("po-eval-chat-badge");
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
            historyBtn.addClickListener(e -> viewDialogs.openHistoryDialog(request));
            return historyBtn;
        }).setHeader("Geçmiş").setWidth("80px").setFlexGrow(0);

        Grid.Column<RequestEntity> ratingCol = grid.addComponentColumn(this::createRatingColumn).setHeader("Sonuç").setWidth("80px").setFlexGrow(0);
        Grid.Column<RequestEntity> slaCol = grid.addComponentColumn(this::createSlaBadge).setHeader("SLA").setWidth("105px").setFlexGrow(0);

        grid.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) viewDialogs.openRequestDetailDialog(event.getItem());
        });

        TextField searchField = new TextField();
        searchField.setPlaceholder("Ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> filterHelper.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(assigneeCol).setComponent(filterHelper.createComboBoxFilterHeader("Filtre"));
        headerRow.getCell(titleCol).setComponent(searchField);
        headerRow.getCell(descCol).setComponent(new Span());
        headerRow.getCell(effortCol).setComponent(new Span()); 
        headerRow.getCell(dateCol).setComponent(filterHelper.createDateRangeFilterHeader());
        headerRow.getCell(screenshotCol).setComponent(new Span());
        headerRow.getCell(scoreCol).setComponent(new Span());
        headerRow.getCell(evalCol).setComponent(new Span());
        headerRow.getCell(chatCol).setComponent(new Span());
        headerRow.getCell(historyCol).setComponent(new Span());
        headerRow.getCell(ratingCol).setComponent(new Span());
        headerRow.getCell(slaCol).setComponent(new Span());
    }

    private void refreshGrid() {
        List<RequestEntity> requests = requestService.getAllRequestsForGrid();
        requests.sort((r1, r2) -> {
            int score1 = (r1.getPrioritization() != null) ? r1.getPrioritization().getPriorityScore() : -1;
            int score2 = (r2.getPrioritization() != null) ? r2.getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1);
        });
        filterHelper.setDataView(grid.setItems(requests)); 
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
        badge.addClassName("po-eval-sla-badge");
        return badge;
    }

    private Component createRatingColumn(RequestEntity request) {
        if (request.getSatisfactionScore() != null) {
            Span pointBadge = new Span("⭐ " + request.getSatisfactionScore());
            pointBadge.getElement().getThemeList().add("badge success");
            pointBadge.addClassName("po-eval-point-badge");
            
            if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                pointBadge.getElement().setProperty("title", getTranslation("helpdesk.triage.commentPrefix") + ": " + request.getSatisfactionComment());
                pointBadge.addClassName("po-eval-point-badge-help");
            }
            return pointBadge;
        } else if ("KAPATILDI".equals(request.getStatus())) {
            return new Span("-");
        }
        return new Span("-");
    }
}