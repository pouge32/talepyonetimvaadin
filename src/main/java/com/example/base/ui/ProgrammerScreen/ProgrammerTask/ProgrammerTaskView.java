package com.example.base.ui.ProgrammerScreen.ProgrammerTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.SubTaskEntity;
import com.example.base.entity.UserEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.SubTaskRepository;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.ChatService;
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "programmer-paneli", layout = MainLayout.class)
@RolesAllowed({"PROGRAMMER","GODPANEL"}) 
@CssImport("./styles/programmer/programmer-task.css")
public class ProgrammerTaskView extends VerticalLayout implements HasDynamicTitle {

    private final WorkflowRepository workflowRepository;
    private final SettingsService settingsService;
    private final ChatService chatService;
    private final SubTaskRepository subTaskRepository; 

    private final Grid<WorkflowEntity> grid = new Grid<>(WorkflowEntity.class, false);
    
    private final ProgrammerTaskFilter filterHelper;
    private final ProgrammerTaskDialogs dialogHelper;
    private final UserEntity currentUser;

    public ProgrammerTaskView(WorkflowRepository workflowRepository, 
                              RequestRepository requestRepository,
                              NotificationService notificationService, 
                              SystemLogService systemLogService,
                              SettingsService settingsService,
                              UserRepository userRepository,
                              SubTaskRepository subTaskRepository,
                              ChatService chatService,
                              InternalCommentService internalCommentService,
                              TeamChatBroadcaster teamChatBroadcaster) {
        this.workflowRepository = workflowRepository;
        this.settingsService = settingsService;
        this.chatService = chatService;
        this.subTaskRepository = subTaskRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("programmer-layout");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        this.filterHelper = new ProgrammerTaskFilter(this);
        this.dialogHelper = new ProgrammerTaskDialogs(this, currentUser, this::refreshGrid, grid.getDataProvider()::refreshItem,
            workflowRepository, requestRepository, subTaskRepository, notificationService, systemLogService, 
            internalCommentService, teamChatBroadcaster);

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("programmer-main-container");

        configureGrid();

        grid.setWidthFull();
        grid.addClassName("programmer-grid");

        mainContainer.add(buildHeader(), grid);
        add(mainContainer);
        
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("programmer.pageTitle");
    }

    private Div buildHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.addClassName("programmer-header");

        H2 title = new H2(getTranslation("programmer.heading"));
        title.addClassName("programmer-heading");

        Paragraph subtitle = new Paragraph(getTranslation("programmer.subtitle"));
        subtitle.addClassName("programmer-subtitle");

        header.add(title, subtitle);
        return header;
    }

    private void configureGrid() {
        Grid.Column<WorkflowEntity> idCol = grid.addColumn(workflow -> workflow.getRequest() != null ? workflow.getRequest().getRequestId() : "-")
                .setHeader(getTranslation("programmer.grid.id")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> titleCol = grid.addColumn(workflow -> workflow.getRequest() != null ? workflow.getRequest().getTitle() : getTranslation("home.unknown"))
                .setHeader(getTranslation("programmer.grid.title")).setWidth("140px").setFlexGrow(1);

        Grid.Column<WorkflowEntity> dateCol = grid.addColumn(workflow -> {
            if (workflow.getAssignedAt() != null) {
                return workflow.getAssignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            }
            return "-";
        }).setHeader(getTranslation("programmer.grid.assignedAt")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> priorityCol = grid.addComponentColumn(this::createPriorityBadge)
                .setHeader(getTranslation("programmer.grid.priority")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> statusCol = grid.addComponentColumn(this::createActionColumn)
                .setHeader(getTranslation("programmer.grid.statusUpdate")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> progressCol = grid.addComponentColumn(workflow -> {
            List<SubTaskEntity> tasks = subTaskRepository.findByWorkflow_TaskIdOrderByIdAsc(workflow.getTaskId());
            
            if (tasks.isEmpty()) {
                Span emptySpan = new Span("-");
                emptySpan.addClassName("programmer-progress-empty");
                return emptySpan;
            }
            
            long completed = tasks.stream().filter(SubTaskEntity::isCompleted).count();
            double percentage = (double) completed / tasks.size();

            ProgressBar progressBar = new ProgressBar();
            progressBar.setValue(percentage);
            progressBar.setWidth("70px");
            
            if (percentage == 1.0) {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            }

            Span label = new Span(Math.round(percentage * 100) + "%");
            label.addClassName("programmer-progress-label");

            HorizontalLayout layout = new HorizontalLayout(progressBar, label);
            layout.setAlignItems(Alignment.CENTER);
            layout.setPadding(false);
            layout.setSpacing(false);
            return layout;
        }).setHeader("İlerleme").setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> effortCol = grid.addColumn(workflow -> {
            return workflow.getActualEffortHours() != null ? workflow.getActualEffortHours() + " Saat" : "-";
        }).setHeader(getTranslation("programmer.grid.effort")).setAutoWidth(true).setFlexGrow(0);
                
        Grid.Column<WorkflowEntity> contentCol = grid.addComponentColumn(workflow -> {
            Button detailBtn = new Button(getTranslation("programmer.btn.detail"), VaadinIcon.INFO_CIRCLE.create());
            detailBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            detailBtn.addClickListener(e -> dialogHelper.showDetails(workflow));
            return detailBtn;
        }).setHeader(getTranslation("programmer.grid.content")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> slaCol = grid.addComponentColumn(this::createSlaBadge).setHeader(getTranslation("programmer.grid.sla")).setAutoWidth(true).setFlexGrow(0);

        grid.addItemDoubleClickListener(event -> {
            WorkflowEntity workflow = event.getItem();
            if (workflow != null) {
                dialogHelper.showDetails(workflow);
            }
        });

        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("programmer.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> filterHelper.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(idCol).setComponent(new Span());
        headerRow.getCell(titleCol).setComponent(searchField);
        headerRow.getCell(dateCol).setComponent(filterHelper.createDateRangeFilterHeader());
        headerRow.getCell(priorityCol).setComponent(filterHelper.createStatusFilterHeader(filterHelper::setMinScoreFilter));
        headerRow.getCell(statusCol).setComponent(new Span());
        headerRow.getCell(progressCol).setComponent(new Span());
        headerRow.getCell(contentCol).setComponent(new Span());
        headerRow.getCell(slaCol).setComponent(new Span());
        headerRow.getCell(effortCol).setComponent(new Span());
    }

    private Badge createSlaBadge(WorkflowEntity workflow) {
        RequestEntity request = workflow.getRequest();
        Badge badge;
        
        if (request == null) {
             badge = new Badge("-");
        } else if ("KAPATILDI".equals(request.getStatus())) {
            badge = new Badge(getTranslation("requests.sla.completed"));
            badge.addThemeVariants(BadgeVariant.CONTRAST); 
        } else {
            long hoursElapsed = ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());
            long slaLimitHours = settingsService.getSlaLimitHours();
            long warningLimitHours = (long) (slaLimitHours * settingsService.getSlaWarningPercent());

            if (hoursElapsed >= slaLimitHours) {
                badge = new Badge(getTranslation("requests.sla.violated") + " (" + hoursElapsed + "s)");
                badge.addThemeVariants(BadgeVariant.ERROR);
                badge.getElement().setProperty("title", getTranslation("requests.sla.violatedTitle"));
            } else if (hoursElapsed >= warningLimitHours) {
                badge = new Badge(getTranslation("requests.sla.warning") + " (" + hoursElapsed + "s)");
                badge.addThemeVariants(BadgeVariant.WARNING);
                badge.getElement().setProperty("title", getTranslation("requests.sla.warningTitle"));
            } else {
                badge = new Badge(getTranslation("requests.sla.normal") + " (" + hoursElapsed + "s)");
                badge.addThemeVariants(BadgeVariant.SUCCESS);
            }
        }
        badge.addClassName("programmer-sla-badge");
        return badge;
    }

    private Span createPriorityBadge(WorkflowEntity workflow) {
        Span badge = new Span();
        if (workflow.getRequest() != null && workflow.getRequest().getPrioritization() != null) {
            int score = workflow.getRequest().getPrioritization().getPriorityScore();
            if (score >= 999) {
                badge.setText(getTranslation("programmer.priority.urgent"));
                badge.getElement().getThemeList().add("badge error");
            } else if (score >= 20) {
                badge.setText(getTranslation("programmer.priority.critical"));
                badge.getElement().getThemeList().add("badge error primary");
            } else if (score >= 10) {
                badge.setText(getTranslation("programmer.priority.high"));
                badge.getElement().getThemeList().add("badge warning");
            } else if (score >= 5) {
                badge.setText(getTranslation("programmer.priority.normal"));
                badge.getElement().getThemeList().add("badge success");
            } else {
                badge.setText(getTranslation("programmer.priority.low"));
                badge.getElement().getThemeList().add("badge contrast");
            }
        } else {
            badge.setText(getTranslation("programmer.priority.unknown"));
            badge.getElement().getThemeList().add("badge contrast");
        }
        badge.addClassName("programmer-priority-badge");
        return badge;
    }

    private HorizontalLayout createActionColumn(WorkflowEntity workflow) {
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems("BACKLOG", "IN DEVELOPMENT", "TEST", "DONE");
        
        String currentStatus = workflow.getWorkflowStatus();

        if ("DONE".equals(currentStatus) && workflow.getRequest() != null && "İş Akışına Dönüştü".equals(workflow.getRequest().getStatus())) {
            currentStatus = "IN DEVELOPMENT";
            workflow.setWorkflowStatus(currentStatus);
            workflowRepository.save(workflow);
        }

        statusCombo.setValue(currentStatus != null ? currentStatus : "BACKLOG");
        statusCombo.setWidth("135px");

        Button saveBtn = new Button(VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        
        final String finalStatusForLambda = currentStatus; 
        
        saveBtn.addClickListener(e -> dialogHelper.updateWorkflowStatus(workflow, statusCombo.getValue(), statusCombo, finalStatusForLambda));

        HorizontalLayout layout = new HorizontalLayout(statusCombo, saveBtn);
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    private void refreshGrid() {
        List<WorkflowEntity> workflows = workflowRepository.findAllWithRequests();
        workflows.sort((w1, w2) -> {
            int score1 = (w1.getRequest() != null && w1.getRequest().getPrioritization() != null) ? w1.getRequest().getPrioritization().getPriorityScore() : -1;
            int score2 = (w2.getRequest() != null && w2.getRequest().getPrioritization() != null) ? w2.getRequest().getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1);
        });

        GridListDataView<WorkflowEntity> dataView = grid.setItems(workflows);
        filterHelper.setDataView(dataView);
    }
}