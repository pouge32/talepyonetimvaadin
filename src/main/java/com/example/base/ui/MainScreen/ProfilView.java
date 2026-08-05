package com.example.base.ui.MainScreen;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.SystemLogService; 
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Route(value = "profil", layout = MainLayout.class)
@RolesAllowed(value = {"CUSTOMER", "HELPDESK", "PO", "ADMIN","PROGRAMMER"})
public class ProfilView extends VerticalLayout implements HasDynamicTitle {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;
    private UserEntity currentUser;
    private String newPhotoUrl = null;

    public ProfilView(UserRepository userRepository, PasswordEncoder passwordEncoder, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H2 title = new H2(getTranslation("profile.title"));
        Span subtitle = new Span(getTranslation("profile.subtitle"));
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = (auth != null) ? auth.getName() : "";

        Optional<UserEntity> userOptional = userRepository.findByEmail(loggedInEmail);
        if (userOptional.isPresent()) {
            currentUser = userOptional.get();
        }

        Avatar avatar = new Avatar();
        avatar.setWidth("100px");
        avatar.setHeight("100px");
        avatar.getStyle().set("margin-right", "20px");
        if (currentUser != null) {
            avatar.setName(currentUser.getNameSurname());
            updateAvatarImage(avatar, currentUser.getProfilePhotoUrl()); 
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
        photoLabel.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");

        FileBuffer buffer = new FileBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setDropLabel(new Span(getTranslation("profile.dropLabel")));

        upload.addSucceededListener(event -> {
            String originalFileName = event.getFileName();
            
            try {
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
                File targetFile = new File(uploadDir, uniqueFileName);

                Files.copy(buffer.getFileData().getFile().toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                newPhotoUrl = "/uploads/" + uniqueFileName;

                Notification.show(getTranslation("profile.notif.tempPhoto"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show(getTranslation("profile.notif.photoError"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout avatarLayout = new HorizontalLayout(avatar, upload);
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

                if (newPhotoUrl != null) {
                    currentUser.setProfilePhotoUrl(newPhotoUrl);
                    updateAvatarImage(avatar, newPhotoUrl); 
                }

                userRepository.save(currentUser);

                systemLogService.log(getTranslation("profile.log.updated") + " (" + loggedInEmail + ") " + getTranslation("profile.log.updatedSuffix"));

                Notification.show(getTranslation("profile.notif.success"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                passwordField.clear();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("margin-top", "20px");

        add(title, subtitle, formLayout, photoLabel, avatarLayout, saveButton);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("profile.pageTitle");
    }

    private void updateAvatarImage(Avatar avatar, String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String filePath = photoUrl.startsWith("/") ? photoUrl.substring(1) : photoUrl;
            File imgFile = new File(filePath);
            
            if (imgFile.exists()) {
                StreamResource resource = new StreamResource(imgFile.getName(), () -> {
                    try {
                        return new FileInputStream(imgFile);
                    } catch (FileNotFoundException e) {
                        return new ByteArrayInputStream(new byte[0]);
                    }
                });
                avatar.setImageResource(resource);
            }
        }
    }
}