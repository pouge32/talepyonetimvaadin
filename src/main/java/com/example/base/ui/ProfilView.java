package com.example.base.ui;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
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
@RolesAllowed(value = {"CUSTOMER", "HELPDESK", "PO", "ADMIN"})
public class ProfilView extends VerticalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private UserEntity currentUser;
    private String newPhotoUrl = null;

    public ProfilView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Profil Ayarları");
        Span subtitle = new Span("Bilgilerinizi güncelleyebilir ve yüzünüzün net göründüğü bir profil fotoğrafı yükleyebilirsiniz.");
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

        TextField nameField = new TextField("Ad Soyad");
        nameField.setValue(currentUser != null ? currentUser.getNameSurname() : "");

        EmailField emailField = new EmailField("E-Posta Adresi");
        emailField.setValue(loggedInEmail);
        emailField.setErrorMessage("Lütfen geçerli bir e-posta adresi giriniz");
        emailField.setClearButtonVisible(true);

        PasswordField passwordField = new PasswordField("Yeni Şifre");
        passwordField.setPlaceholder("Değiştirmek istemiyorsanız boş bırakın");

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, emailField, passwordField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.setMaxWidth("500px");

        Span photoLabel = new Span("Profil Fotoğrafı (Lütfen yüzünüzün net bir şekilde göründüğü bir fotoğraf yükleyin):");
        photoLabel.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");

        FileBuffer buffer = new FileBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setDropLabel(new Span("Fotoğrafı buraya sürükleyin veya seçin"));

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

                Notification.show("Fotoğraf geçici olarak belleğe alındı. İşlemi tamamlamak için Kaydet'e basın.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Fotoğraf kaydedilirken hata oluştu!", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout avatarLayout = new HorizontalLayout(avatar, upload);
        avatarLayout.setAlignItems(Alignment.CENTER);

        Button saveButton = new Button("Değişiklikleri Kaydet", e -> {
            if (currentUser != null) {
                if (emailField.isInvalid()) {
                    Notification.show("Lütfen geçerli bir e-posta adresi girin!", 3000, Notification.Position.TOP_CENTER)
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

                Notification.show("Profil bilgileriniz başarıyla güncellendi.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                passwordField.clear();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("margin-top", "20px");

        add(title, subtitle, formLayout, photoLabel, avatarLayout, saveButton);
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