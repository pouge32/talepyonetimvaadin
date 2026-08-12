package com.example.base.ui.AdminScreen;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.SystemLogService; 
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/kullanicilar", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "GODPANEL"})
@CssImport("./styles/admin/admin-user-managment.css")
public class AdminUserManagmentView extends VerticalLayout implements HasDynamicTitle {

    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final PasswordEncoder passwordEncoder;
    private final Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

    private final Dialog banDialog = new Dialog();
    private final TextArea banReasonField = new TextArea();
    private UserEntity targetUser;

    private final Dialog userFormDialog = new Dialog();
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final ComboBox<Role> roleField = new ComboBox<>();
    private UserEntity editingUser = null;

    private final Dialog deleteDialog = new Dialog();
    private UserEntity deletingUser = null;

    public AdminUserManagmentView(UserRepository userRepository, SystemLogService systemLogService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("admin-users-layout");

        H3 title = new H3(getTranslation("admin.users.headerTitle"));
        title.addClassName("admin-users-title");

        Button newUserBtn = new Button(getTranslation("admin.users.btn.newUser"), VaadinIcon.USER.create());
        newUserBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newUserBtn.addClickListener(e -> openUserForm(null));

        HorizontalLayout headerRow = new HorizontalLayout(title, newUserBtn);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

        configureGrid();
        configureBanDialog();
        configureUserFormDialog();
        configureDeleteDialog();

        VerticalLayout container = new VerticalLayout(headerRow, grid);
        container.setSizeFull();
        container.addClassName("admin-users-container");

        grid.setWidthFull();
        grid.addClassName("admin-users-grid");

        add(container);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.users.pageTitle");
    }

    private void configureGrid() {
        grid.addColumn(UserEntity::getUserId).setHeader(getTranslation("admin.users.grid.id")).setAutoWidth(true);
        grid.addColumn(UserEntity::getNameSurname).setHeader(getTranslation("admin.users.field.name")).setAutoWidth(true);
        grid.addColumn(UserEntity::getEmail).setHeader(getTranslation("admin.users.grid.email")).setAutoWidth(true);

        grid.addColumn(user -> user.getRole() != null ? user.getRole().name() : "-")
            .setHeader(getTranslation("admin.users.grid.role"))
            .setAutoWidth(true);

        grid.addComponentColumn(user -> {
            boolean isBanned = user.isBanned();
            
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openUserForm(user));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteBtn.addClickListener(e -> {
                deletingUser = user;
                deleteDialog.open();
            });

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

            HorizontalLayout actions = new HorizontalLayout(editBtn, deleteBtn, actionButton);
            actions.setAlignItems(FlexComponent.Alignment.CENTER);
            return actions;
        }).setHeader(getTranslation("admin.users.grid.action")).setAutoWidth(true);

        grid.setWidthFull();
    }

    private void openUserForm(UserEntity user) {
        editingUser = user;
        if (user != null) {
            userFormDialog.setHeaderTitle(getTranslation("admin.users.dialog.editTitle"));
            nameField.setValue(user.getNameSurname() != null ? user.getNameSurname() : "");
            emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
            passwordField.clear();
            roleField.setValue(user.getRole());
        } else {
            userFormDialog.setHeaderTitle(getTranslation("admin.users.dialog.newTitle"));
            nameField.clear();
            emailField.clear();
            passwordField.clear();
            roleField.setValue(Role.CUSTOMER);
        }
        userFormDialog.open();
    }

    private void configureUserFormDialog() {
        nameField.setLabel(getTranslation("admin.users.field.name"));
        nameField.setWidthFull();

        emailField.setLabel(getTranslation("admin.users.field.email"));
        emailField.setWidthFull();

        passwordField.setLabel(getTranslation("admin.users.field.password"));
        passwordField.setWidthFull();

        roleField.setLabel(getTranslation("admin.users.field.role"));
        roleField.setItems(Role.values());
        roleField.setWidthFull();

        Button saveBtn = new Button(getTranslation("admin.users.btn.save"), event -> {
            try {
                String rawPassword = passwordField.getValue();
                String encodedPassword = (rawPassword != null && !rawPassword.isEmpty()) 
                    ? passwordEncoder.encode(rawPassword) 
                    : passwordEncoder.encode("123456");

                if (editingUser == null) {
                    UserEntity newUser = new UserEntity();
                    newUser.setNameSurname(nameField.getValue());
                    newUser.setEmail(emailField.getValue());
                    
                    newUser.setPassword(encodedPassword);
                    try {
                        java.lang.reflect.Method setHashMethod = UserEntity.class.getMethod("setPasswordHash", String.class);
                        setHashMethod.invoke(newUser, encodedPassword);
                    } catch (Exception ignored) {}

                    newUser.setRole(roleField.getValue() != null ? roleField.getValue() : Role.CUSTOMER);
                    newUser.setStatus("APPROVED");
                    userRepository.save(newUser);

                    systemLogService.log("Admin yeni kullanıcı oluşturdu: " + newUser.getEmail());
                } else {
                    editingUser.setNameSurname(nameField.getValue());
                    editingUser.setEmail(emailField.getValue());
                    
                    if (rawPassword != null && !rawPassword.isEmpty()) {
                        editingUser.setPassword(encodedPassword);
                        try {
                            java.lang.reflect.Method setHashMethod = UserEntity.class.getMethod("setPasswordHash", String.class);
                            setHashMethod.invoke(editingUser, encodedPassword);
                        } catch (Exception ignored) {}
                    }
                    if (roleField.getValue() != null) {
                        editingUser.setRole(roleField.getValue());
                    }
                    userRepository.save(editingUser);

                    systemLogService.log("Admin, " + editingUser.getEmail() + " kullanıcısının bilgilerini güncelledi.");
                }

                Notification.show(getTranslation("admin.users.notification.saved"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                userFormDialog.close();
                refreshGrid();
            } catch (Exception e) {
                Notification.show(getTranslation("admin.users.notification.errorPrefix") + e.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> userFormDialog.close());

        userFormDialog.getFooter().add(saveBtn, cancelBtn);
        userFormDialog.add(new VerticalLayout(nameField, emailField, passwordField, roleField));
    }

    private void configureDeleteDialog() {
        deleteDialog.setHeaderTitle(getTranslation("admin.users.dialog.deleteTitle"));
        deleteDialog.add(new Paragraph(getTranslation("admin.users.dialog.deleteText")));

        Button confirmDeleteBtn = new Button(getTranslation("admin.users.btn.delete"), e -> {
            if (deletingUser != null) {
                userRepository.delete(deletingUser);
                systemLogService.log("Admin, " + deletingUser.getEmail() + " kullanıcısını sildi.");
                
                Notification.show(getTranslation("admin.users.notification.deleted"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                deleteDialog.close();
                deletingUser = null;
                refreshGrid();
            }
        });
        confirmDeleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelDeleteBtn = new Button(getTranslation("admin.management.btn.cancel"), e -> deleteDialog.close());
        deleteDialog.getFooter().add(confirmDeleteBtn, cancelDeleteBtn);
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