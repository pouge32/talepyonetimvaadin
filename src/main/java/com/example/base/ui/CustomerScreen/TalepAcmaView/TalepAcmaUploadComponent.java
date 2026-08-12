package com.example.base.ui.CustomerScreen.TalepAcmaView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.base.service.SettingsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

public class TalepAcmaUploadComponent extends Div {

    private final SettingsService settingsService;
    private final MemoryBuffer uploadBuffer = new MemoryBuffer();
    private final Upload screenshotUpload = new Upload(uploadBuffer);
    private final VerticalLayout previewsListLayout = new VerticalLayout();
    
    private final List<byte[]> uploadedBytesList = new ArrayList<>();
    private final List<String> uploadedFileNames = new ArrayList<>();
    private final List<String> uploadedMimeTypes = new ArrayList<>();

    public TalepAcmaUploadComponent(SettingsService settingsService) {
        this.settingsService = settingsService;
        setWidthFull();
        addClassName("talep-acma-screenshot-wrapper");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(true);
        
        Icon camIcon = VaadinIcon.CAMERA.create();
        camIcon.setSize("16px");
        camIcon.addClassName("talep-acma-camera-icon");
        
        Span label = new Span(getTranslation("request.create.screenshotsLabel"));
        label.addClassName("talep-acma-screenshot-label");
        
        labelRow.add(camIcon, label);

        screenshotUpload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        screenshotUpload.setMaxFiles(10); 
        
        int maxBytes = settingsService.getMaxFileUploadSize() * 1024 * 1024;
        screenshotUpload.setMaxFileSize(maxBytes);
        screenshotUpload.setDropAllowed(true);
        screenshotUpload.setWidthFull();
        screenshotUpload.addClassName("talep-acma-upload");

        screenshotUpload.setUploadButton(new Button(getTranslation("request.create.uploadBtn"), VaadinIcon.UPLOAD.create()));

        Span dropLabel = new Span(getTranslation("request.create.dropLabel") + " (Max: " + settingsService.getMaxFileUploadSize() + " MB)");
        dropLabel.addClassName("talep-acma-drop-label");
        screenshotUpload.setDropLabel(dropLabel);

        screenshotUpload.addSucceededListener(event -> {
            try (InputStream inputStream = uploadBuffer.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int read;
                while ((read = inputStream.read(data)) != -1) {
                    buffer.write(data, 0, read);
                }
                uploadedBytesList.add(buffer.toByteArray());
                uploadedFileNames.add(event.getFileName());
                uploadedMimeTypes.add(event.getMIMEType());
                renderPreviews();
                Notification.show(getTranslation("request.create.notif.added") + event.getFileName(), 2000, Notification.Position.TOP_CENTER);
            } catch (IOException e) {
                Notification.show(getTranslation("request.create.notif.readError") + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            }
        });

        screenshotUpload.addFileRejectedListener(event -> {
            Notification error = Notification.show(event.getErrorMessage(), 4000, Notification.Position.TOP_CENTER);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        previewsListLayout.setPadding(false);
        previewsListLayout.setSpacing(true);
        previewsListLayout.setWidthFull();
        previewsListLayout.addClassName("talep-acma-previews-layout");

        add(labelRow, screenshotUpload, previewsListLayout);
    }

    private void renderPreviews() {
        previewsListLayout.removeAll();
        for (int i = 0; i < uploadedFileNames.size(); i++) {
            final int index = i;
            Div itemCard = new Div();
            itemCard.setWidthFull();
            itemCard.addClassName("talep-acma-preview-card");

            HorizontalLayout info = new HorizontalLayout();
            info.setAlignItems(FlexComponent.Alignment.CENTER);
            info.setSpacing(true);

            Icon imgIcon = VaadinIcon.FILE_PICTURE.create();
            imgIcon.getStyle().set("color", "var(--lumo-primary-color)");
            Span nameSpan = new Span(uploadedFileNames.get(i));
            nameSpan.addClassName("talep-acma-preview-name");
            info.add(imgIcon, nameSpan);

            Button removeBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                uploadedBytesList.remove(index);
                uploadedFileNames.remove(index);
                uploadedMimeTypes.remove(index);
                renderPreviews();
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            itemCard.add(info, removeBtn);
            previewsListLayout.add(itemCard);
        }
    }

    public void clearScreenshots() {
        uploadedBytesList.clear();
        uploadedFileNames.clear();
        uploadedMimeTypes.clear();
        screenshotUpload.clearFileList();
        previewsListLayout.removeAll();
    }

    public List<byte[]> getUploadedBytesList() { return uploadedBytesList; }
    public List<String> getUploadedFileNames() { return uploadedFileNames; }
    public List<String> getUploadedMimeTypes() { return uploadedMimeTypes; }
}