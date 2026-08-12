package com.example.base.ui.CustomerScreen.Taleplerim;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.ChatService;
import com.example.base.service.RequestService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "taleplerim", layout = MainLayout.class)
@RolesAllowed(value = {"CUSTOMER", "GODPANEL"})
@CssImport("./styles/customer/taleplerim.css")
public class TaleplerimView extends VerticalLayout implements HasDynamicTitle {

    private final RequestService requestService;
    private final TaleplerimGridComponent gridComponent;

    public TaleplerimView(RequestService requestService, ChatService chatService, 
                          UserRepository userRepository, SystemLogService systemLogService,
                          RequestRepository requestRepository, SettingsService settingsService,
                          WorkflowRepository workflowRepository) { 
        this.requestService = requestService;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        systemLogService.log("Müşteri (" + email + ") taleplerim sayfasını görüntüledi.");

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("taleplerim-layout");

        TaleplerimDialogHelper dialogHelper = new TaleplerimDialogHelper(this, requestService, workflowRepository, this::refreshGrid);
        
        this.gridComponent = new TaleplerimGridComponent(chatService, userRepository, systemLogService, 
                                                         settingsService, workflowRepository, dialogHelper);

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("taleplerim-main-container");

        mainContainer.add(buildPageHeader(), gridComponent);
        add(mainContainer);

        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("requests.pageTitle");
    }

    private Div buildPageHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.addClassName("taleplerim-header");

        H2 heading = new H2(getTranslation("requests.heading"));
        heading.addClassName("taleplerim-heading");

        Paragraph subtitle = new Paragraph(getTranslation("requests.subtitle"));
        subtitle.addClassName("taleplerim-subtitle");

        header.add(heading, subtitle);
        return header;
    }

    private void refreshGrid() {
        List<RequestEntity> requests = requestService.getMyRequestsForCurrentUser();
        requests.sort((r1, r2) -> {
            int score1 = (r1.getPrioritization() != null) ? r1.getPrioritization().getPriorityScore() : -1;
            int score2 = (r2.getPrioritization() != null) ? r2.getPrioritization().getPriorityScore() : -1;
            return Integer.compare(score2, score1); 
        });
        gridComponent.setGridItems(requests);
    }
}