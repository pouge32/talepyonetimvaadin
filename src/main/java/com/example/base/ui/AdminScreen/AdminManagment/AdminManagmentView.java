package com.example.base.ui.AdminScreen.AdminManagment;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.ChatService;
import com.example.base.service.DemoDataService;
import com.example.base.service.ExcelExportService;
import com.example.base.service.InternalCommentService;
import com.example.base.service.NotificationService;
import com.example.base.service.PdfExportService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.service.TeamChatBroadcaster;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin-paneli", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "GODPANEL"})
@CssImport("./styles/admin/admin-managment.css")
public class AdminManagmentView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final SystemLogService systemLogService;
    private final NotificationService notificationService;
    private final AdminGridComponent gridComponent;

    public AdminManagmentView(RequestService requestService, NotificationService notificationService,
                              ChatService chatService, UserRepository userRepository,
                              RequestRepository requestRepository, SystemLogService systemLogService,
                              ExcelExportService excelExportService, PdfExportService pdfExportService,
                              DemoDataService demoDataService, WorkflowRepository workflowRepository,
                              InternalCommentService internalCommentService, TeamChatBroadcaster teamChatBroadcaster) { 
        this.requestRepository = requestRepository;
        this.systemLogService = systemLogService;
        this.notificationService = notificationService;

        AdminDialogsHelper dialogsHelper = new AdminDialogsHelper(requestService, requestRepository, workflowRepository, userRepository, 
                                                                  systemLogService, notificationService, internalCommentService, 
                                                                  teamChatBroadcaster, this::refreshGrid);

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("admin-view-layout");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("admin-main-container");

        this.gridComponent = new AdminGridComponent(workflowRepository, userRepository, chatService, 
                                                    systemLogService, dialogsHelper, this::forwardToPo);
        
        AdminBulkActionComponent bulkActionBar = new AdminBulkActionComponent(gridComponent, requestRepository, systemLogService, notificationService, this::refreshGrid);
        
        gridComponent.setSelectionMode(Grid.SelectionMode.MULTI);
        gridComponent.addSelectionListener(event -> bulkActionBar.updateSelectionCount(event.getAllSelectedItems().size()));

        AdminHeaderComponent header = new AdminHeaderComponent(demoDataService, requestService, requestRepository, 
                                                               systemLogService, excelExportService, pdfExportService, this::refreshGrid);
        
        mainContainer.add(header, bulkActionBar, gridComponent);
        add(mainContainer);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.management.pageTitle"); 
    }

    private void refreshGrid() {
        List<RequestEntity> sortedRequests = requestRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        sortedRequests.sort((r1, r2) -> {
            int score1 = (r1.getPrioritization() != null) ? r1.getPrioritization().getPriorityScore() : -1;
            int score2 = (r2.getPrioritization() != null) ? r2.getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1); 
        });
        gridComponent.setGridItems(sortedRequests);
    }

    private void forwardToPo(RequestEntity request) {
        request.setStatus("INCELEMEDE");
        requestRepository.save(request);
        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebini PO'ya sevk etti.");
        Notification.show(getTranslation("admin.notification.poForwarded"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }
}