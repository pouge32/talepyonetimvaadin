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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/kullanicilar", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminUserManagmentView extends VerticalLayout implements HasDynamicTitle {

    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

    private final Dialog banDialog = new Dialog();
    private final TextArea banReasonField = new TextArea();
    private UserEntity targetUser;

    public AdminUserManagmentView(UserRepository userRepository, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H3 title = new H3(getTranslation("admin.users.headerTitle"));
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        configureGrid();
        configureBanDialog();

        VerticalLayout container = new VerticalLayout(title, grid);
        container.setSizeFull();
        container.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "20px");

        grid.setWidthFull();
        grid.getStyle().set("flex-grow", "1").set("border-radius", "12px");

        add(container);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.users.pageTitle");
    }

    private void configureGrid() {
        grid.addColumn(UserEntity::getUserId).setHeader(getTranslation("admin.users.grid.id")).setAutoWidth(true);
        grid.addColumn(UserEntity::getEmail).setHeader(getTranslation("admin.users.grid.email")).setAutoWidth(true);

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

                        Notification.show(getTranslation("admin.users.notification.roleUpdated"), 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception e) {
                        Notification.show(getTranslation("admin.users.notification.errorPrefix") + e.getMessage(), 4000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
            });
            return roleBox;
        }).setHeader(getTranslation("admin.users.grid.role")).setAutoWidth(true);

        grid.addComponentColumn(user -> {
            boolean isBanned = user.isBanned();
            
            Button actionButton = new Button(isBanned ? getTranslation("admin.users.btn.activate") : getTranslation("admin.users.btn.ban"));
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
                    Notification.show(getTranslation("admin.users.notification.unbanned"), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    targetUser = user;
                    banReasonField.clear();
                    banDialog.open();
                }
            });

            return actionButton;
        }).setHeader(getTranslation("admin.users.grid.action")).setAutoWidth(true);

        grid.setWidthFull();
    }

    private void configureBanDialog() {
        banDialog.setHeaderTitle(getTranslation("admin.users.banDialog.title"));
        banReasonField.setLabel(getTranslation("admin.users.banDialog.reasonLabel"));
        banReasonField.setWidthFull();
        banReasonField.setPlaceholder(getTranslation("admin.users.banDialog.placeholder"));

        Button confirmBtn = new Button(getTranslation("admin.users.banDialog.confirm"), event -> {
            if (targetUser != null) {
                targetUser.setBanned(true);
                targetUser.setBanReason(banReasonField.getValue());
                userRepository.save(targetUser);

                systemLogService.log("Admin, " + targetUser.getEmail() + " kullanıcısını banladı. Gerekçe: " + banReasonField.getValue());

                Notification.show(getTranslation("admin.users.notification.banned"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                
                banDialog.close();
                targetUser = null;
                refreshGrid();
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> banDialog.close());

        banDialog.getFooter().add(confirmBtn, cancelBtn);
        banDialog.add(banReasonField);
    }

    private void refreshGrid() {
        grid.setItems(userRepository.findAll());
    }
}