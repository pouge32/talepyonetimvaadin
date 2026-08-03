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
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "musteri-onay", layout = MainLayout.class)
@RolesAllowed("HELPDESK")
public class MusteriOnayView extends VerticalLayout {

    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

    public MusteriOnayView(UserRepository userRepository, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        add(new H3("Onay Bekleyen Müşteriler"));
        
        configureGrid();
        add(grid);
        
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(UserEntity::getUserId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(UserEntity::getNameSurname).setHeader("Ad Soyad");
        grid.addColumn(UserEntity::getEmail).setHeader("E-Posta");
        grid.addColumn(UserEntity::getStatus).setHeader("Statü");

        grid.addComponentColumn(user -> {
            Button approveBtn = new Button("Onayla", VaadinIcon.CHECK.create());
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            approveBtn.addClickListener(e -> approveUser(user));

            Button rejectBtn = new Button("Reddet", VaadinIcon.CLOSE.create());
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            rejectBtn.addClickListener(e -> rejectUser(user));

            return new HorizontalLayout(approveBtn, rejectBtn);
        }).setHeader("İşlem").setAutoWidth(true);
    }

    private void approveUser(UserEntity user) {
        user.setStatus("AKTIF");
        userRepository.save(user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String staffEmail = (auth != null) ? auth.getName() : "";
        systemLogService.log("Destek Personeli (" + staffEmail + "), " + user.getEmail() + " müşterisinin kaydını onayladı.");
        
        Notification.show(user.getNameSurname() + " başarıyla onaylandı ve aktif edildi!", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
        refreshGrid();
    }

    private void rejectUser(UserEntity user) {
        String userEmail = user.getEmail();
        userRepository.delete(user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String staffEmail = (auth != null) ? auth.getName() : "";
        systemLogService.log("Destek Personeli (" + staffEmail + "), " + userEmail + " müşterisinin kaydını reddetti ve sildi.");
        
        Notification.show(user.getNameSurname() + " kaydı reddedildi ve silindi.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(userRepository.findByStatus("ONAY_BEKLIYOR"));
    }
}