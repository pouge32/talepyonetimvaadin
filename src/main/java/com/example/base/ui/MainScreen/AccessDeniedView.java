package com.example.base.ui.MainScreen;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasErrorParameter;

import jakarta.annotation.security.PermitAll;

@PermitAll
@CssImport("./styles/main/access-denied.css")
public class AccessDeniedView extends VerticalLayout implements HasErrorParameter<AccessDeniedException>, HasDynamicTitle {

    private final Span errorMessage = new Span();
    private final SystemLogService systemLogService;

    public AccessDeniedView(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
        addClassName("access-denied-layout");

        H1 baslik = new H1(getTranslation("error.403.title"));
        baslik.addClassName("access-denied-title");

        Span aciklama = new Span(getTranslation("error.403.description"));
        
        errorMessage.addClassName("access-denied-error-msg");
        
        Button anasayfaButonu = new Button(getTranslation("error.403.homeButton"), VaadinIcon.HOME.create());
        anasayfaButonu.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));
        anasayfaButonu.addClassName("access-denied-home-btn");

        add(baslik, aciklama, errorMessage, anasayfaButonu);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("error.403.pageTitle");
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        String targetPath = "/" + event.getLocation().getPath();
        errorMessage.setText(getTranslation("error.403.targetPathPrefix") + ": " + targetPath);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (auth != null && auth.isAuthenticated()) ? auth.getName() : getTranslation("error.403.anonymous");
        systemLogService.log(getTranslation("error.403.logPrefix") + " " + userEmail + " " + getTranslation("error.403.logInfix") + " '" + targetPath + "' " + getTranslation("error.403.logSuffix"));

        return 403;
    }
}