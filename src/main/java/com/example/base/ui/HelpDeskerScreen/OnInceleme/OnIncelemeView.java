package com.example.base.ui.HelpDeskerScreen.OnInceleme;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
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
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "on-inceleme", layout = MainLayout.class)
@RolesAllowed({"HELPDESK", "GODPANEL"})
@CssImport("./styles/helpdesker/on-inceleme.css")
public class OnIncelemeView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    
    private final OnIncelemeFilter requestFilter = new OnIncelemeFilter();
    private final OnIncelemeGridComponent gridComponent;
    
    private final UserEntity currentUser;

    public OnIncelemeView(RequestRepository requestRepository, RequestService requestService, NotificationService notificationService,
                          ChatService chatService, UserRepository userRepository,
                          SystemLogService systemLogService, SettingsService settingsService,
                          WorkflowRepository workflowRepository,
                          InternalCommentService internalCommentService, TeamChatBroadcaster teamChatBroadcaster) { 
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        boolean isGod = currentUser != null && currentUser.getRole() != null && "GODPANEL".equals(currentUser.getRole().name());
        requestFilter.setGodPanel(isGod);

        OnIncelemeDialogHelper dialogHelper = new OnIncelemeDialogHelper(this, requestRepository, notificationService, 
                                                                         systemLogService, workflowRepository, internalCommentService, 
                                                                         teamChatBroadcaster, currentUser, this::refreshGrid);

        gridComponent = new OnIncelemeGridComponent(chatService, systemLogService, settingsService, workflowRepository, 
                                                    dialogHelper, currentUser, this::forwardToPo);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H3 title = new H3(getTranslation("helpdesk.triage.headerTitle"));
        title.addClassName("on-inceleme-title");

        Tab tabMine = new Tab(getTranslation("helpdesk.triage.tab.assignedToMe"));
        Tab tabAll = new Tab(getTranslation("helpdesk.triage.tab.allPool"));
        Tabs tabs = new Tabs(tabMine, tabAll);
        tabs.addClassName("on-inceleme-tabs");
        
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

        add(gridComponent);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("helpdesk.triage.pageTitle");
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
            notificationService.notifyUser(request.getCustomer().getUserId(), 
                    getTranslation("helpdesk.triage.notif.underReviewTitle"), 
                    getTranslation("helpdesk.triage.notif.underReviewDesc"));
        }
        Notification.show(getTranslation("helpdesk.triage.notif.forwarded"), 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }

    private void refreshGrid() {
        List<RequestEntity> requests = requestService.getAllRequestsForGrid();
        requests.sort((r1, r2) -> {
            int score1 = (r1.getPrioritization() != null) ? r1.getPrioritization().getPriorityScore() : -1;
            int score2 = (r2.getPrioritization() != null) ? r2.getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1);
        });
        gridComponent.setGridItems(requests, requestFilter);
    }
}