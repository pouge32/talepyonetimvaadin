package com.example.base.ui.MainScreen.ProfilScreen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.server.StreamResource;

public class ProfilAvatarManager {

    private final Avatar avatar;
    private String newPhotoUrl = null;

    public ProfilAvatarManager() {
        this.avatar = new Avatar();
        this.avatar.setWidth("100px");
        this.avatar.setHeight("100px");
        this.avatar.addClassName("profil-avatar");
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public String getNewPhotoUrl() {
        return newPhotoUrl;
    }

    public void updateAvatarImage(String photoUrl) {
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

    public Upload createUploadComponent(Component context) {
        FileBuffer buffer = new FileBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setDropLabel(new Span(context.getTranslation("profile.dropLabel")));

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

                Notification.show(context.getTranslation("profile.notif.tempPhoto"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show(context.getTranslation("profile.notif.photoError"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        return upload;
    }
}