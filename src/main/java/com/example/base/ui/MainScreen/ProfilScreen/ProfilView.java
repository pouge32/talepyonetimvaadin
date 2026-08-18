package com.example.base.ui.MainScreen.ProfilScreen;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.SystemLogService;
import com.example.base.service.UserService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "profil", layout = MainLayout.class)
@RolesAllowed(value = {"CUSTOMER", "HELPDESK", "PO", "ADMIN", "PROGRAMMER", "GODPANEL"})
@CssImport("./styles/main/profil.css")
public class ProfilView extends VerticalLayout implements HasDynamicTitle {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;
    private UserEntity currentUser;

    public ProfilView(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                      SystemLogService systemLogService, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        addClassName("profil-layout");

        H2 title = new H2(getTranslation("profile.title"));
        Span subtitle = new Span(getTranslation("profile.subtitle"));
        subtitle.addClassName("profil-subtitle");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = (auth != null) ? auth.getName() : "";

        Optional<UserEntity> userOptional = userRepository.findByEmail(loggedInEmail);
        userOptional.ifPresent(userEntity -> currentUser = userEntity);

        ProfilAvatarManager avatarManager = new ProfilAvatarManager();
        if (currentUser != null) {
            avatarManager.getAvatar().setName(currentUser.getNameSurname());
            avatarManager.updateAvatarImage(currentUser.getProfilePhotoUrl()); 
        }

        TextField nameField = new TextField(getTranslation("profile.name"));
        nameField.setValue(currentUser != null ? currentUser.getNameSurname() : "");

        EmailField emailField = new EmailField(getTranslation("profile.email"));
        emailField.setValue(loggedInEmail);
        emailField.setErrorMessage(getTranslation("profile.emailError"));
        emailField.setClearButtonVisible(true);

        PasswordField passwordField = new PasswordField(getTranslation("profile.newPassword"));
        passwordField.setPlaceholder(getTranslation("profile.passwordPlaceholder"));

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, emailField, passwordField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.setMaxWidth("500px");

        Span photoLabel = new Span(getTranslation("profile.photoLabel"));
        photoLabel.addClassName("profil-photo-label");

        Upload upload = avatarManager.createUploadComponent(this);

        HorizontalLayout avatarLayout = new HorizontalLayout(avatarManager.getAvatar(), upload);
        avatarLayout.setAlignItems(Alignment.CENTER);

        Button saveButton = new Button(getTranslation("profile.saveButton"), e -> {
            if (currentUser != null) {
                if (emailField.isInvalid()) {
                    Notification.show(getTranslation("profile.notif.invalidEmail"), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                currentUser.setNameSurname(nameField.getValue());
                currentUser.setEmail(emailField.getValue());

                if (!passwordField.getValue().isEmpty()) {
                    String newHashedPassword = passwordEncoder.encode(passwordField.getValue());
                    currentUser.setPasswordHash(newHashedPassword);
                }

                if (avatarManager.getNewPhotoUrl() != null) {
                    currentUser.setProfilePhotoUrl(avatarManager.getNewPhotoUrl());
                    avatarManager.updateAvatarImage(avatarManager.getNewPhotoUrl()); 
                }

                userRepository.save(currentUser);

                systemLogService.log(getTranslation("profile.log.updated") + " (" + loggedInEmail + ") " + getTranslation("profile.log.updatedSuffix"));

                Notification.show(getTranslation("profile.notif.success"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                passwordField.clear();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.addClassName("profil-action-buttons");
        actionButtons.setJustifyContentMode(JustifyContentMode.BETWEEN);

        if (currentUser != null) {
            ProfilKvkkHelper kvkkHelper = new ProfilKvkkHelper(userService, this);
            Button kvkkButton = kvkkHelper.buildKvkkDeleteButton(currentUser);
            actionButtons.add(saveButton, kvkkButton);
        } else {
            actionButtons.add(saveButton);
        }

        add(title, subtitle, formLayout, photoLabel, avatarLayout, actionButtons);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("profile.pageTitle");
    }
}