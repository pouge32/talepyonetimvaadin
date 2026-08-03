package com.example.base.ui.AdminScreen;

import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.SystemLogService; 
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/kullanicilar", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminUserManagmentView extends VerticalLayout {

    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

    private Dialog banDialog = new Dialog();
    private TextArea banReasonField = new TextArea("Ban / Kapatılma Nedeni");
    private UserEntity targetUser;

    public AdminUserManagmentView(UserRepository userRepository, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        add(new H3("Kullanıcı Yönetim Paneli"));

        configureGrid();
        configureBanDialog();

        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(UserEntity::getUserId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(UserEntity::getEmail).setHeader("E-Posta").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            ComboBox<Role> roleBox = new ComboBox<>();
            roleBox.setItems(Role.values());
            
            if (user.getRole() != null) {
                roleBox.setValue(user.getRole());
            }

            roleBox.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    try {
                        Role eskiRol = user.getRole();
                        user.setRole(event.getValue());
                        userRepository.save(user);
                        
                        systemLogService.log("Admin, " + user.getEmail() + " kullanıcısının rolünü " + eskiRol + " -> " + event.getValue() + " olarak güncelledi.");

                        Notification.show("Kullanıcı rolü güncellendi.", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception e) {
                        Notification.show("Hata: " + e.getMessage(), 4000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
            });
            return roleBox;
        }).setHeader("Rol").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            boolean isBanned = user.isBanned();
            
            Button actionButton = new Button(isBanned ? "Hesabı Aktif Et" : "Hesabı Banla / Kapat");
            if (isBanned) {
                actionButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            } else {
                actionButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            }

            actionButton.addClickListener(e -> {
                if (isBanned) {
                    user.setBanned(false);
                    user.setBanReason(null);
                    userRepository.save(user);

                    systemLogService.log("Admin, " + user.getEmail() + " kullanıcısının banını kaldırdı.");

                    refreshGrid();
                    Notification.show("Kullanıcının banı kaldırıldı.", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    targetUser = user;
                    banReasonField.clear();
                    banDialog.open();
                }
            });

            return actionButton;
        }).setHeader("İşlem / Durum").setAutoWidth(true);

        grid.setWidthFull();
    }

    private void configureBanDialog() {
        banDialog.setHeaderTitle("Hesabı Kapat / Banla");
        banReasonField.setWidthFull();
        banReasonField.setPlaceholder("Örn: Kural ihlali nedeniyle hesabınız kapatılmıştır.");

        Button confirmBtn = new Button("Banla ve Kaydet", event -> {
            if (targetUser != null) {
                targetUser.setBanned(true);
                targetUser.setBanReason(banReasonField.getValue());
                userRepository.save(targetUser);

                systemLogService.log("Admin, " + targetUser.getEmail() + " kullanıcısını banladı. Gerekçe: " + banReasonField.getValue());

                Notification.show("Kullanıcı banlandı.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                
                banDialog.close();
                targetUser = null;
                refreshGrid();
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("İptal", e -> banDialog.close());

        banDialog.getFooter().add(confirmBtn, cancelBtn);
        banDialog.add(banReasonField);
    }

    private void refreshGrid() {
        grid.setItems(userRepository.findAll());
    }
}