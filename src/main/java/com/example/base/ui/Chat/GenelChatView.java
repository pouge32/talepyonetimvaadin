package com.example.base.ui.Chat;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.GlobalChatMessageEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.GlobalChatService;
import com.example.base.service.GlobalChatBroadcaster;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "genel-chat", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "PO", "HELPDESK", "PROGRAMMER", "GODPANEL"})
@CssImport("./styles/chat/genel-chat.css")
public class GenelChatView extends VerticalLayout implements HasDynamicTitle {

    private final GlobalChatService chatService;
    private final GlobalChatBroadcaster broadcaster;
    private final UserRepository userRepository;

    private final VerticalLayout messageLayout = new VerticalLayout();
    private final Scroller scroller = new Scroller(messageLayout);
    private final TextField inputField = new TextField();

    private byte[] uploadedFileData = null;
    private String uploadedFileName = null;
    private final Span fileIndicator = new Span();
    private Upload upload;

    private UserEntity currentUser;
    private java.util.function.Consumer<GlobalChatMessageEntity> broadcastListener;

    public GenelChatView(GlobalChatService chatService, GlobalChatBroadcaster broadcaster, UserRepository userRepository) {
        this.chatService = chatService;
        this.broadcaster = broadcaster;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("genel-chat-layout");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("genel-chat-main-container");

        H2 heading = new H2("Genel Ekip Sohbeti");
        heading.addClassName("genel-chat-heading");
        
        Paragraph subtitle = new Paragraph("Tüm yetkili personelin (Admin, PO, Destek, Yazılımcı) ortak genel iletişim kanalı.");
        subtitle.addClassName("genel-chat-subtitle");

        messageLayout.setPadding(true);
        messageLayout.setSpacing(true);
        scroller.setWidthFull();
        scroller.addClassName("genel-chat-scroller");

        inputField.setPlaceholder("Genel ekibe yazın... Veya isme tıklayarak / Rol yazarak fısıldayın.");
        inputField.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setAutoUpload(true);
        
        Button uploadButton = new Button(VaadinIcon.PAPERCLIP.create());
        uploadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        uploadButton.getElement().setProperty("title", "Dosya Ekle");
        upload.setUploadButton(uploadButton);
        upload.setDropLabel(new Span(""));
        upload.addClassName("genel-chat-upload");

        upload.addSucceededListener(event -> {
            try {
                uploadedFileName = event.getFileName();
                uploadedFileData = buffer.getInputStream().readAllBytes();
                fileIndicator.setText("📎 " + uploadedFileName + " eklendi");
                fileIndicator.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        upload.addFileRemovedListener(event -> {
            uploadedFileData = null;
            uploadedFileName = null;
            fileIndicator.setVisible(false);
        });

        fileIndicator.setVisible(false);
        fileIndicator.addClassName("genel-chat-file-indicator");

        Button sendBtn = new Button(VaadinIcon.PAPERPLANE.create());
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendBtn.addClickListener(e -> sendMessage());
        inputField.addKeyPressListener(Key.ENTER, e -> sendMessage());

        HorizontalLayout inputLayout = new HorizontalLayout(upload, fileIndicator, inputField, sendBtn);
        inputLayout.setWidthFull();
        inputLayout.setAlignItems(Alignment.CENTER);
        inputLayout.setPadding(false);
        inputLayout.addClassName("genel-chat-input-layout");

        mainContainer.add(heading, subtitle, scroller, inputLayout);
        add(mainContainer);

        loadMessages();
    }

    @Override
    public String getPageTitle() {
        return "Genel Sohbet";
    }

    private void loadMessages() {
        messageLayout.removeAll();
        if (currentUser == null) return;
        List<GlobalChatMessageEntity> messages = chatService.getVisibleMessagesForUser(currentUser.getUserId());
        for (GlobalChatMessageEntity msg : messages) {
            messageLayout.add(createMessageBubble(msg));
        }
        
        chatService.markAllAsReadForUser(currentUser.getUserId());
        UI.getCurrent().getChildren()
              .filter(c -> c instanceof MainLayout)
              .map(c -> (MainLayout) c)
              .findFirst()
              .ifPresent(MainLayout::updateGlobalChatBadge);
              
        scrollToBottom();
    }

    private HorizontalLayout createMessageBubble(GlobalChatMessageEntity message) {
        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setAlignItems(Alignment.START);
        main.addClassName("genel-chat-message-bubble");

        Avatar avatar = new Avatar(message.getSender().getNameSurname());

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.BASELINE);
        header.setSpacing(true);

        Span name = new Span(message.getSender().getNameSurname());
        name.addClassName("genel-chat-sender-name");
        
        name.addClickListener(e -> {
            inputField.setValue("/msg " + message.getSender().getNameSurname() + " ");
            inputField.focus();
        });

        String roleStr = message.getSender().getRole() != null ? message.getSender().getRole().name() : "USER";
        Badge roleBadge = new Badge(roleStr);
        roleBadge.addClassName("genel-chat-role-badge");

        if (roleStr.equals("ADMIN") || roleStr.equals("GODPANEL")) {
            roleBadge.addThemeVariants(BadgeVariant.ERROR);
        } else if (roleStr.equals("PO")) {
            roleBadge.addThemeVariants(BadgeVariant.WARNING);
        } else if (roleStr.equals("PROGRAMMER")) {
            roleBadge.addThemeVariants(BadgeVariant.SUCCESS);
        } else {
            roleBadge.addThemeVariants(BadgeVariant.CONTRAST);
        }

        Span date = new Span(message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        date.addClassName("genel-chat-date");

        header.add(name, roleBadge, date);

        if (message.getReceiver() != null) {
            Span whisperLabel = new Span(message.getSender().getUserId().equals(currentUser.getUserId()) 
                ? "(Şuna fısıldadın: " + message.getReceiver().getNameSurname() + ")" 
                : "(Sana fısıldadı)");
            whisperLabel.addClassName("genel-chat-whisper-label");
            header.add(whisperLabel);
        }

        Div text = new Div();
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            text.setText(message.getContent());
            text.addClassName("genel-chat-message-text");
        }

        body.add(header);
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            body.add(text);
        }

        if (message.getFileData() != null && message.getFileData().length > 0) {
            StreamResource resource = new StreamResource(
                message.getFileName() != null ? message.getFileName() : "dosya",
                () -> new ByteArrayInputStream(message.getFileData())
            );
            Anchor downloadLink = new Anchor(resource, "📥 " + (message.getFileName() != null ? message.getFileName() : "Dosyayı İndir"));
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.addClassName("genel-chat-download-link");
            body.add(downloadLink);
        }

        main.add(avatar, body);
        return main;
    }

    private void sendMessage() {
        String text = inputField.getValue();
        if ((text == null || text.trim().isEmpty()) && uploadedFileData == null) return;
        
        chatService.sendMessage(currentUser.getUserId(), text, uploadedFileData, uploadedFileName);
        
        inputField.clear();
        uploadedFileData = null;
        uploadedFileName = null;
        fileIndicator.setVisible(false);
        upload.clearFileList();
    }

    private void scrollToBottom() {
        UI.getCurrent().getPage().executeJs("setTimeout(() => { $0.scrollTop = $0.scrollHeight; }, 100);", scroller.getElement());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        broadcastListener = message -> {
            if (message.getReceiver() == null || 
                message.getSender().getUserId().equals(currentUser.getUserId()) || 
                message.getReceiver().getUserId().equals(currentUser.getUserId())) {
                
                ui.access(() -> {
                    messageLayout.add(createMessageBubble(message));
                    scrollToBottom();
                    
                    if (currentUser != null) {
                        chatService.markAllAsReadForUser(currentUser.getUserId());
                    }
                });
            }
        };
        broadcaster.register(broadcastListener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcaster.unregister(broadcastListener);
        super.onDetach(detachEvent);
    }
}