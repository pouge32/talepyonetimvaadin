package com.example.base.ui.Chat;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.base.entity.InternalCommentEntity;
import com.example.base.entity.UserEntity;
import com.example.base.service.InternalCommentService;
import com.example.base.service.TeamChatBroadcaster;
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;

@CssImport("./styles/chat/internal-chat-panel.css")
public class InternalChatPanel extends VerticalLayout {

    private final InternalCommentService commentService;
    private final TeamChatBroadcaster broadcaster;
    private final Integer requestId;
    private final UserEntity currentUser;

    private final VerticalLayout messageLayout = new VerticalLayout();
    private final Scroller scroller = new Scroller(messageLayout);
    private final TextField inputField = new TextField();

    private byte[] uploadedFileData = null;
    private String uploadedFileName = null;
    private final Span fileIndicator = new Span();

    private java.util.function.Consumer<InternalCommentEntity> broadcastListener;

    public InternalChatPanel(InternalCommentService commentService, TeamChatBroadcaster broadcaster,
                             Integer requestId, UserEntity currentUser) {
        this.commentService = commentService;
        this.broadcaster = broadcaster;
        this.requestId = requestId;
        this.currentUser = currentUser;

        addClassName("internal-chat-panel");
        setPadding(false);
        setSpacing(false);

        messageLayout.setPadding(true);
        messageLayout.setSpacing(true);
        scroller.setWidthFull();
        scroller.addClassName("internal-chat-scroller");

        inputField.setPlaceholder("Takıma yazın veya '/msg İsim' ile fısıldayın...");
        inputField.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setAutoUpload(true);
        
        Button uploadButton = new Button(VaadinIcon.PAPERCLIP.create());
        uploadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        uploadButton.getElement().setProperty("title", "Dosya Ekle (Sürükleyip bırakabilirsiniz)");
        upload.setUploadButton(uploadButton);
        
        upload.setDropLabel(new Span(""));
        upload.addClassName("internal-chat-upload");

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

        fileIndicator.setVisible(false);
        fileIndicator.addClassName("internal-chat-file-indicator");

        Button sendBtn = new Button(VaadinIcon.PAPERPLANE.create());
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendBtn.addClickListener(e -> sendMessage());
        inputField.addKeyPressListener(Key.ENTER, e -> sendMessage());

        HorizontalLayout inputLayout = new HorizontalLayout(upload, fileIndicator, inputField, sendBtn);
        inputLayout.setWidthFull();
        inputLayout.setAlignItems(Alignment.CENTER);
        inputLayout.setPadding(false);
        inputLayout.addClassName("internal-chat-input-layout");

        add(scroller, inputLayout);
        loadMessages();
    }

    private void loadMessages() {
        messageLayout.removeAll();
        List<InternalCommentEntity> comments = commentService.getVisibleComments(requestId, currentUser.getUserId());
        for (InternalCommentEntity comment : comments) {
            messageLayout.add(createMessageBubble(comment));
        }
        commentService.markAsRead(requestId, currentUser.getUserId());
        scrollToBottom();
    }

    private HorizontalLayout createMessageBubble(InternalCommentEntity comment) {
        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setAlignItems(Alignment.START);
        main.addClassName("internal-chat-message-bubble");

        Avatar avatar = new Avatar(comment.getSender().getNameSurname());

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.BASELINE);
        header.setSpacing(true);

        Span name = new Span(comment.getSender().getNameSurname());
        name.addClassName("internal-chat-sender-name");
        
        name.addClickListener(e -> {
            inputField.setValue("/msg " + comment.getSender().getNameSurname() + " ");
            inputField.focus();
        });

        String roleStr = comment.getSender().getRole() != null ? comment.getSender().getRole().name() : "USER";
        Badge roleBadge = new Badge(roleStr);
        roleBadge.addClassName("internal-chat-role-badge");

        if (roleStr.equals("ADMIN") || roleStr.equals("GODPANEL")) {
            roleBadge.addThemeVariants(BadgeVariant.ERROR);
        } else if (roleStr.equals("PO")) {
            roleBadge.addThemeVariants(BadgeVariant.WARNING);
        } else if (roleStr.equals("PROGRAMMER")) {
            roleBadge.addThemeVariants(BadgeVariant.SUCCESS);
        } else {
            roleBadge.addThemeVariants(BadgeVariant.CONTRAST);
        }

        Span date = new Span(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        date.addClassName("internal-chat-date");

        header.add(name, roleBadge, date);

        if (comment.getReceiver() != null) {
            Span whisperLabel = new Span(comment.getSender().getUserId().equals(currentUser.getUserId()) 
                ? "(Şuna fısıldadın: " + comment.getReceiver().getNameSurname() + ")" 
                : "(Sana fısıldadı)");
            whisperLabel.addClassName("internal-chat-whisper-label");
            header.add(whisperLabel);
        }

        Div text = new Div();
        if (comment.getContent() != null && !comment.getContent().isEmpty()) {
            text.setText(comment.getContent());
            text.addClassName("internal-chat-message-text");
        }

        body.add(header);
        if (comment.getContent() != null && !comment.getContent().isEmpty()) {
            body.add(text);
        }

        if (comment.getFileData() != null && comment.getFileData().length > 0) {
            StreamResource resource = new StreamResource(
                comment.getFileName() != null ? comment.getFileName() : "dosya",
                () -> new ByteArrayInputStream(comment.getFileData())
            );
            Anchor downloadLink = new Anchor(resource, "📥 " + (comment.getFileName() != null ? comment.getFileName() : "Dosyayı İndir"));
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.addClassName("internal-chat-download-link");
            body.add(downloadLink);
        }

        main.add(avatar, body);
        return main;
    }

    private void sendMessage() {
        String text = inputField.getValue();
        if ((text == null || text.trim().isEmpty()) && uploadedFileData == null) return;
        
        commentService.sendCommentWithFile(requestId, currentUser.getUserId(), text, uploadedFileData, uploadedFileName);
        
        inputField.clear();
        uploadedFileData = null;
        uploadedFileName = null;
        fileIndicator.setVisible(false);
    }

    private void scrollToBottom() {
        UI.getCurrent().getPage().executeJs("setTimeout(() => { $0.scrollTop = $0.scrollHeight; }, 100);", scroller.getElement());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        broadcastListener = comment -> {
            if (comment.getReceiver() == null || 
                comment.getSender().getUserId().equals(currentUser.getUserId()) || 
                comment.getReceiver().getUserId().equals(currentUser.getUserId())) {
                
                ui.access(() -> {
                    messageLayout.add(createMessageBubble(comment));
                    scrollToBottom();
                    commentService.markAsRead(requestId, currentUser.getUserId());
                });
            }
        };
        broadcaster.registerForRequest(requestId, broadcastListener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcaster.unregisterForRequest(requestId, broadcastListener);
        super.onDetach(detachEvent);
    }
}