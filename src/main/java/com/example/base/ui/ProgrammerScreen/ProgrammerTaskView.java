package com.example.base.ui.ProgrammerScreen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

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
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "programmer-paneli", layout = MainLayout.class)
@RolesAllowed({"PROGRAMMER","GODPANEL"}) 
public class ProgrammerTaskView extends VerticalLayout implements HasDynamicTitle {

    private final WorkflowRepository workflowRepository;
    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final SettingsService settingsService;
    private final UserRepository userRepository; 
    private final SubTaskRepository subTaskRepository; 
    
    private final InternalCommentService internalCommentService;
    private final TeamChatBroadcaster teamChatBroadcaster;

    private final Grid<WorkflowEntity> grid = new Grid<>(WorkflowEntity.class, false);
    private GridListDataView<WorkflowEntity> dataView;
    private final WorkflowFilter workflowFilter = new WorkflowFilter();

    public ProgrammerTaskView(WorkflowRepository workflowRepository, 
                              RequestRepository requestRepository,
                              NotificationService notificationService, 
                              SystemLogService systemLogService,
                              SettingsService settingsService,
                              UserRepository userRepository,
                              SubTaskRepository subTaskRepository,
                              InternalCommentService internalCommentService,
                              TeamChatBroadcaster teamChatBroadcaster) {
        this.workflowRepository = workflowRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.subTaskRepository = subTaskRepository;
        this.internalCommentService = internalCommentService;
        this.teamChatBroadcaster = teamChatBroadcaster;

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
                .set("padding", "20px")
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-sizing", "border-box");

        configureGrid();

        grid.setWidthFull();
        grid.getStyle()
                .set("flex-grow", "1")
                .set("border-radius", "12px")
                .set("margin-top", "16px");

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
        header.getStyle().set("flex-shrink", "0");

        H2 title = new H2(getTranslation("programmer.heading"));
        title.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-header-text-color)");

        Paragraph subtitle = new Paragraph(getTranslation("programmer.subtitle"));
        subtitle.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

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
                emptySpan.getStyle().set("color", "var(--lumo-disabled-text-color)");
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
            label.getStyle().set("font-size", "12px").set("font-weight", "bold").set("margin-left", "8px");

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
            detailBtn.addClickListener(e -> showDetails(workflow));
            return detailBtn;
        }).setHeader(getTranslation("programmer.grid.content")).setAutoWidth(true).setFlexGrow(0);

        Grid.Column<WorkflowEntity> slaCol = grid.addComponentColumn(this::createSlaBadge).setHeader(getTranslation("programmer.grid.sla")).setAutoWidth(true).setFlexGrow(0);

        grid.addItemDoubleClickListener(event -> {
            WorkflowEntity workflow = event.getItem();
            if (workflow != null) {
                showDetails(workflow);
            }
        });

        TextField searchField = new TextField();
        searchField.setPlaceholder(getTranslation("programmer.filter.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> workflowFilter.setSearchTerm(e.getValue()));

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(idCol).setComponent(new Span());
        headerRow.getCell(titleCol).setComponent(searchField);
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(workflowFilter));
        headerRow.getCell(priorityCol).setComponent(createStatusFilterHeader(workflowFilter::setMinScoreFilter));
        headerRow.getCell(statusCol).setComponent(new Span());
        headerRow.getCell(progressCol).setComponent(new Span());
        headerRow.getCell(contentCol).setComponent(new Span());
        headerRow.getCell(slaCol).setComponent(new Span());
        headerRow.getCell(effortCol).setComponent(new Span());
    }

    private Component createStatusFilterHeader(Consumer<Integer> filterChangeConsumer) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setItems(999, 20, 10, 5);
        comboBox.setItemLabelGenerator(score -> {
            if (score >= 999) return getTranslation("programmer.combobox.Priority.urgent");
            if (score >= 20) return getTranslation("programmer.combobox.Priority.critical");
            if (score >= 10) return getTranslation("programmer.combobox.Priority.high");
            return getTranslation("programmer.combobox.Priority.normal");
        });
        comboBox.setPlaceholder("Öncelik");
        comboBox.setClearButtonVisible(true);
        comboBox.setWidth("110px");
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private Component createDateRangeFilterHeader(WorkflowFilter workflowFilter) {
        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder("Başlangıç");
        startPicker.setClearButtonVisible(true);
        startPicker.setWidth("90px");
        startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        startPicker.setValue(LocalDate.now().minusWeeks(1));
        workflowFilter.setStartDate(LocalDate.now().minusWeeks(1).atStartOfDay());

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder("Bitiş");
        endPicker.setClearButtonVisible(true);
        endPicker.setWidth("90px");
        endPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        endPicker.setValue(LocalDate.now());
        workflowFilter.setEndDate(LocalDate.now().atTime(23, 59, 59));

        startPicker.addValueChangeListener(e -> {
            endPicker.setMin(e.getValue());
            workflowFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });

        endPicker.addValueChangeListener(e -> {
            startPicker.setMax(e.getValue());
            workflowFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        HorizontalLayout layout = new HorizontalLayout(startPicker, endPicker);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("gap", "2px");
        return layout;
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
        badge.getStyle().set("min-width", "90px").set("justify-content", "center").set("white-space", "nowrap");
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
        badge.getStyle().set("min-width", "70px").set("display", "inline-flex").set("justify-content", "center");
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
        
        saveBtn.addClickListener(e -> updateWorkflowStatus(workflow, statusCombo.getValue(), statusCombo, finalStatusForLambda));

        HorizontalLayout layout = new HorizontalLayout(statusCombo, saveBtn);
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    private void updateWorkflowStatus(WorkflowEntity workflow, String newStatus, ComboBox<String> statusCombo, String oldStatus) {
        if (newStatus == null || newStatus.equals(workflow.getWorkflowStatus())) {
            return;
        }

        if ("DONE".equals(newStatus)) {
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Görevi Tamamla ve Detay Gir");
            confirmDialog.setWidth("500px");
            
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.add(new Paragraph("Yazılım sürecini tamamlayıp PO onayına aktarmak üzeresiniz. Lütfen gerekli alanları doldurunuz."));

            IntegerField effortField = new IntegerField("Harcanan Toplam Efor (Saat)");
            effortField.setMin(1);
            effortField.setStepButtonsVisible(true);
            effortField.setRequiredIndicatorVisible(true);
            effortField.setWidthFull();

            TextField prLinkField = new TextField("PR Linki / Commit ID (Opsiyonel)");
            prLinkField.setPlaceholder("Örn: https://github.com/.../pull/12 veya 7a8f9c...");
            prLinkField.setWidthFull();
            prLinkField.setClearButtonVisible(true);

            TextArea noteField = new TextArea("Tamamlama Notu / Açıklama (Opsiyonel)");
            noteField.setPlaceholder("Karşılaşılan zorluklar veya PO'nun bilmesi gereken teknik notlar...");
            noteField.setWidthFull();

            layout.add(effortField, prLinkField, noteField);
            confirmDialog.add(layout);

            Button confirmBtn = new Button("Tamamla ve Gönder", event -> {
                if (effortField.getValue() == null || effortField.getValue() <= 0) {
                    Notification.show("Lütfen harcanan eforu saat cinsinden belirtiniz!", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                workflow.setActualEffortHours(effortField.getValue());
                executeStatusUpdate(workflow, newStatus, effortField.getValue(), noteField.getValue(), prLinkField.getValue());
                confirmDialog.close();
            });
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

            Button cancelBtn = new Button("İptal", event -> {
                statusCombo.setValue(oldStatus); 
                confirmDialog.close();
            });
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            confirmDialog.getFooter().add(confirmBtn, cancelBtn);
            confirmDialog.open();
        } else {
            executeStatusUpdate(workflow, newStatus, null, null, null);
        }
    }

    private void executeStatusUpdate(WorkflowEntity workflow, String newStatus, Integer effort, String note, String prLink) {
        try {
            workflow.setWorkflowStatus(newStatus);
            
            if (prLink != null && !prLink.trim().isEmpty()) {
                workflow.setPrLink(prLink.trim());
            }

            workflowRepository.save(workflow);

            RequestEntity request = workflow.getRequest();
            
            if ("DONE".equals(newStatus) && request != null) {
                request.setStatus("PO_KONTROL");
                requestRepository.save(request);
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String devEmail = (auth != null) ? auth.getName() : "";
            
            String logMsg = getTranslation("programmer.log.updated") + " (" + devEmail + ") " + 
                            getTranslation("programmer.log.taskId") + ": " + workflow.getTaskId() + " " + 
                            getTranslation("programmer.log.status") + ": " + newStatus;
                            
            if ("DONE".equals(newStatus) && effort != null) {
                logMsg += " | Harcanan Efor: " + effort + " Saat";
                
                if (prLink != null && !prLink.trim().isEmpty()) {
                    logMsg += " | PR/Commit: " + prLink;
                }
                if (note != null && !note.trim().isEmpty()) {
                    logMsg += " | Geliştirici Notu: " + note;
                }
            }
            systemLogService.log(logMsg); 
            
            if (request != null && request.getCustomer() != null) {
                String notifMessage = "DONE".equals(newStatus) 
                        ? "Talebinizin yazılım süreci tamamlandı, kalite ve test kontrolü için Ürün Yöneticisi'ne (PO) aktarıldı." 
                        : getTranslation("programmer.notif.message") + " " + newStatus;
                notificationService.notifyUser(request.getCustomer().getUserId(), getTranslation("programmer.notif.title"), notifMessage);
            }

            Notification.show(getTranslation("programmer.notif.success"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            refreshGrid();

        } catch (Exception ex) {
            Notification.show("Hata: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshChecklist(VerticalLayout tasksContainer, WorkflowEntity workflow) {
        tasksContainer.removeAll();
        List<SubTaskEntity> tasks = subTaskRepository.findByWorkflow_TaskIdOrderByIdAsc(workflow.getTaskId());
        for (SubTaskEntity task : tasks) {
            Checkbox checkbox = new Checkbox(task.getDescription(), task.isCompleted());
            
            if (task.isCompleted()) {
                checkbox.getStyle().set("text-decoration", "line-through").set("color", "var(--lumo-disabled-text-color)");
            }
            
            checkbox.addValueChangeListener(e -> {
                task.setCompleted(e.getValue());
                subTaskRepository.save(task);
                
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
                String statusText = e.getValue() ? "tamamlandı" : "bekliyor";
                systemLogService.log("Yazılımcı (" + userEmail + "), Alt Görev: '" + task.getDescription() + "' -> " + statusText);
                
                if (e.getValue()) {
                    checkbox.getStyle().set("text-decoration", "line-through").set("color", "var(--lumo-disabled-text-color)");
                } else {
                    checkbox.getStyle().remove("text-decoration");
                    checkbox.getStyle().remove("color");
                }
                
                grid.getDataProvider().refreshItem(workflow);
            });
            tasksContainer.add(checkbox);
        }
    }

    private void showDetails(WorkflowEntity workflow) {
        Dialog detailDialog = new Dialog(); 
        detailDialog.setHeaderTitle(getTranslation("programmer.dialog.title"));
        detailDialog.setWidth("650px"); 
        detailDialog.setMaxHeight("85vh"); 

        Tabs tabs = new Tabs();
        Tab detayTab = new Tab("Talep Bilgileri");
        Tab yazismaTab = new Tab("İç Yazışma (Takım)");
        tabs.add(detayTab, yazismaTab);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        if (workflow.getRequest() != null) {
            
            String rejectionReason = null;
            try {
                var logs = systemLogService.getLogsForRequest(workflow.getRequest().getRequestId());
                if (logs != null) {
                    for (var log : logs) {
                        String actionText = log.getAction();
                        if (actionText != null && (actionText.contains("REDDETTİ") || actionText.toLowerCase().contains("reddetti"))) {
                            int idx = actionText.indexOf("Gerekçe:");
                            if (idx != -1) {
                                rejectionReason = actionText.substring(idx + 8).trim();
                            } else {
                                rejectionReason = actionText; 
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (rejectionReason != null) {
                Span rejectBadge = new Span("PO Tarafından Kalite Kontrolü Reddedildi");
                rejectBadge.getElement().getThemeList().add("badge error");
                rejectBadge.getStyle().set("margin-bottom", "10px").set("font-size", "14px");
                
                TextArea rejectArea = new TextArea("PO Ret Gerekçesi / İstenen Düzeltmeler");
                rejectArea.setValue(rejectionReason);
                rejectArea.setReadOnly(true);
                rejectArea.setWidthFull();
                rejectArea.getStyle().set("color", "var(--lumo-error-text-color)");
                
                content.add(rejectBadge, rejectArea);
            }

            Span title = new Span(getTranslation("programmer.dialog.titleLabel") + ": " + workflow.getRequest().getTitle());
            title.getStyle().set("font-weight", "bold");
            
            TextArea desc = new TextArea(getTranslation("programmer.dialog.descLabel"));
            desc.setValue(workflow.getRequest().getDescription() != null ? workflow.getRequest().getDescription() : "");
            desc.setReadOnly(true);
            desc.setWidthFull();
            desc.setMinHeight("100px");
            
            content.add(title, desc);

            content.add(new com.vaadin.flow.component.html.Hr());
            Span checklistTitle = new Span("Alt Görevler (Checklist)");
            checklistTitle.getStyle().set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
            content.add(checklistTitle);

            VerticalLayout tasksContainer = new VerticalLayout();
            tasksContainer.setPadding(false);
            tasksContainer.setSpacing(false);

            refreshChecklist(tasksContainer, workflow);

            HorizontalLayout addTaskLayout = new HorizontalLayout();
            TextField newTaskField = new TextField();
            newTaskField.setPlaceholder("Örn: Tabloların oluşturulması...");
            newTaskField.setWidthFull();
            Button addTaskBtn = new Button("Ekle", VaadinIcon.PLUS.create());
            addTaskBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            
            addTaskBtn.addClickListener(e -> {
                if (newTaskField.getValue() != null && !newTaskField.getValue().trim().isEmpty()) {
                    SubTaskEntity newTask = new SubTaskEntity();
                    newTask.setWorkflow(workflow);
                    newTask.setDescription(newTaskField.getValue().trim());
                    newTask.setCompleted(false);
                    subTaskRepository.save(newTask);
                    
                    newTaskField.clear();
                    refreshChecklist(tasksContainer, workflow); 
                    
                    grid.getDataProvider().refreshItem(workflow);
                }
            });
            addTaskLayout.add(newTaskField, addTaskBtn);
            addTaskLayout.setWidthFull();
            addTaskLayout.setAlignItems(Alignment.BASELINE);

            content.add(tasksContainer, addTaskLayout);

            if (workflow.getPrLink() != null && !workflow.getPrLink().isEmpty()) {
                Span prBadge = new Span("🔗 PR / Commit: " + workflow.getPrLink());
                prBadge.getElement().getThemeList().add("badge contrast");
                prBadge.getStyle().set("margin-top", "10px").set("font-size", "14px");
                content.add(prBadge);
            }

            if (workflow.getActualEffortHours() != null) {
                Span effortBadge = new Span("⏱️ Harcanan Efor: " + workflow.getActualEffortHours() + " Saat");
                effortBadge.getElement().getThemeList().add("badge success primary");
                effortBadge.getStyle().set("margin-top", "10px").set("font-size", "14px");
                content.add(effortBadge);
            }

            try {
                var logs = systemLogService.getLogsForRequest(workflow.getRequest().getRequestId());
                if (logs != null && !logs.isEmpty()) {
                    content.add(new com.vaadin.flow.component.html.Hr());
                    Span historyTitle = new Span("Talep Geçmişi");
                    historyTitle.getStyle().set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
                    content.add(historyTitle);

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
                        content.add(stepItem);
                    }
                }
            } catch (Exception ignored) {}

        } else {
            content.add(new Span(getTranslation("programmer.dialog.noDetail")));
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByEmail(email).orElse(null);

        com.example.base.ui.Chat.InternalChatPanel chatPanel = 
            new com.example.base.ui.Chat.InternalChatPanel(internalCommentService, teamChatBroadcaster, workflow.getRequest().getRequestId(), currentUser);
        chatPanel.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            content.setVisible(event.getSelectedTab().equals(detayTab));
            chatPanel.setVisible(event.getSelectedTab().equals(yazismaTab));
        });

        detailDialog.add(tabs, content, chatPanel);

        Button closeBtn = new Button(getTranslation("requests.btn.close"), e -> detailDialog.close());
        detailDialog.getFooter().add(closeBtn);
        
        detailDialog.open();
    }

    private void refreshGrid() {
        List<WorkflowEntity> workflows = workflowRepository.findAllWithRequests();
        workflows.sort((w1, w2) -> {
            int score1 = (w1.getRequest() != null && w1.getRequest().getPrioritization() != null) ? w1.getRequest().getPrioritization().getPriorityScore() : -1;
            int score2 = (w2.getRequest() != null && w2.getRequest().getPrioritization() != null) ? w2.getRequest().getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1);
        });

        dataView = grid.setItems(workflows);
        workflowFilter.setDataView(dataView);
    }
    
    private static class WorkflowFilter {
        private GridListDataView<WorkflowEntity> dataView;
        private String searchTerm = "";
        private Integer minScoreFilter = null;
        private LocalDateTime startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
        private LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);

        public void setDataView(GridListDataView<WorkflowEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setMinScoreFilter(Integer minScoreFilter) {
            this.minScoreFilter = minScoreFilter;
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

        public boolean test(WorkflowEntity workflow) {
            boolean matchesSearch = true;
            if (!searchTerm.isEmpty()) {
                RequestEntity request = workflow.getRequest();
                if (request == null) return false;
                boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
                boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
                boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
                matchesSearch = inTitle || inDesc || inId;
            }

            boolean matchesStatus = true;
            if (minScoreFilter != null) {
                if (workflow.getRequest() == null || workflow.getRequest().getPrioritization() == null) {
                    matchesStatus = false;
                } else {
                    int score = workflow.getRequest().getPrioritization().getPriorityScore();
                    if (minScoreFilter == 999) {
                        matchesStatus = (score >= 999);
                    } else if (minScoreFilter == 20) {
                        matchesStatus = (score >= 20 && score < 999);
                    } else if (minScoreFilter == 10) {
                        matchesStatus = (score >= 10 && score < 20);
                    } else if (minScoreFilter == 5) {
                        matchesStatus = (score >= 5 && score < 10);
                    }
                }
            }

            boolean matchesDate = true;
            if (workflow.getAssignedAt() != null) {
                if (startDate != null && workflow.getAssignedAt().isBefore(startDate)) matchesDate = false;
                if (endDate != null && workflow.getAssignedAt().isAfter(endDate)) matchesDate = false;
            }

            return matchesSearch && matchesStatus && matchesDate;
        }
    }
}