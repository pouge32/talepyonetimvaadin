package com.example.base.ui.HelpDeskerScreen;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.SystemLogService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "musteri-onay", layout = MainLayout.class)
@RolesAllowed({"HELPDESK", "GODPANEL"})
public class MusteriOnayView extends VerticalLayout implements HasDynamicTitle {

    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

    public MusteriOnayView(UserRepository userRepository, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H3 title = new H3(getTranslation("helpdesk.approval.headerTitle"));
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");
        
        configureGrid();
        grid.setWidthFull();

        add(title, grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("helpdesk.approval.pageTitle");
    }

    private void configureGrid() {
        grid.addColumn(UserEntity::getUserId).setHeader(getTranslation("helpdesk.approval.grid.id")).setAutoWidth(true);
        grid.addColumn(UserEntity::getNameSurname).setHeader(getTranslation("helpdesk.approval.grid.name"));
        grid.addColumn(UserEntity::getEmail).setHeader(getTranslation("helpdesk.approval.grid.email"));
        grid.addColumn(UserEntity::getStatus).setHeader(getTranslation("helpdesk.approval.grid.status"));

        grid.addComponentColumn(user -> {
            Button approveBtn = new Button(getTranslation("helpdesk.approval.btn.approve"), VaadinIcon.CHECK.create());
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            approveBtn.addClickListener(e -> approveUser(user));

            Button rejectBtn = new Button(getTranslation("helpdesk.approval.btn.reject"), VaadinIcon.CLOSE.create());
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            rejectBtn.addClickListener(e -> rejectUser(user));

            return new HorizontalLayout(approveBtn, rejectBtn);
        }).setHeader(getTranslation("helpdesk.approval.grid.action")).setAutoWidth(true);
    }

    private void approveUser(UserEntity user) {
        user.setStatus("AKTIF");
        userRepository.save(user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String staffEmail = (auth != null) ? auth.getName() : "";
        systemLogService.log("Destek Personeli (" + staffEmail + "), " + user.getEmail() + " müşterisinin kaydını onayladı.");
        
        Notification.show(user.getNameSurname() + " " + getTranslation("helpdesk.approval.notif.approved"), 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        refreshGrid();
    }

    private void rejectUser(UserEntity user) {
        String userEmail = user.getEmail();
        userRepository.delete(user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String staffEmail = (auth != null) ? auth.getName() : "";
        systemLogService.log("Destek Personeli (" + staffEmail + "), " + userEmail + " müşterisinin kaydını reddetti ve sildi.");
        
        Notification.show(user.getNameSurname() + " " + getTranslation("helpdesk.approval.notif.rejected"), 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(userRepository.findByStatus("ONAY_BEKLIYOR"));
    }
}