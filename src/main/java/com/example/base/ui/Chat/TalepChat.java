package com.example.base.ui.Chat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.MessageEntity;
import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatBroadcaster;
import com.example.base.service.ChatService;
import com.example.base.service.SystemLogService; 
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-chat", layout = MainLayout.class)
@RolesAllowed({"CUSTOMER", "HELPDESK", "PO", "ADMIN", "GODPANEL"})
@CssImport("./styles/chat/talep-chat.css")
public class TalepChat extends VerticalLayout implements HasUrlParameter<Integer>, HasDynamicTitle {

    private final ChatService chatService;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService; 

    private final VerticalLayout messageArea = new VerticalLayout();
    private final TextField input = new TextField();
    private final Button sendButton = new Button();
    private final Upload fileUpload; 
    private final FileBuffer fileBuffer = new FileBuffer();

    private Integer requestId;
    private RequestEntity currentRequest; 
    private UserEntity currentUser;
    private Integer otherPartyId;
    private ChatBroadcaster.Registration registration;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public TalepChat(ChatService chatService, RequestRepository requestRepository,
                       UserRepository userRepository, SystemLogService systemLogService) {
        this.chatService = chatService;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        setSizeFull();
        
        H3 headerTitle = new H3(getTranslation("chat.headerTitle"));
        add(headerTitle);

        messageArea.setSizeFull();
        messageArea.addClassName("talep-chat-message-area");

        input.setPlaceholder(getTranslation("chat.inputPlaceholder"));
        input.setWidthFull();
        input.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> sendMessage());

        sendButton.setText(getTranslation("chat.sendButton"));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());

        fileUpload = new Upload(fileBuffer);
        Button uploadButton = new Button(VaadinIcon.PAPERCLIP.create());
        uploadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        fileUpload.setUploadButton(uploadButton);
        fileUpload.setDropAllowed(true);

        Span dropText = new Span("chat.upload.text");
        dropText.addClassName("talep-chat-drop-text");
        fileUpload.setDropLabel(dropText);

        Icon dropIcon = VaadinIcon.UPLOAD.create();
        dropIcon.setColor("white");
        fileUpload.setDropLabelIcon(dropIcon);

        fileUpload.setAcceptedFileTypes("image/jpeg", "image/png", "application/pdf", ".doc", ".docx", ".txt", ".zip", ".rar");
        
        fileUpload.addSucceededListener(event -> {
            if (otherPartyId == null) {
                Notification.show(getTranslation("chat.error.otherPartyNotFound"), 3000, Notification.Position.MIDDLE);
                return;
            }
            
            try {
                String fileName = event.getFileName();
                InputStream inputStream = fileBuffer.getInputStream();
                byte[] fileBytes = inputStream.readAllBytes();
                
                chatService.sendFileMessage(requestId, currentUser.getUserId(), otherPartyId, fileName, fileBytes);
                
                systemLogService.log("Kullanıcı (" + currentUser.getEmail() + ") ID: " + requestId + " olan talep sohbetine dosya yükledi: " + fileName);
                
                fileUpload.clearFileList();
                
            } catch (Exception ex) {
                Notification.show("Dosya işlenirken hata oluştu: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout inputBar = new HorizontalLayout(fileUpload, input, sendButton);
        inputBar.setWidthFull();
        inputBar.setAlignItems(FlexComponent.Alignment.CENTER);
        inputBar.setFlexGrow(1, input);

        add(messageArea, inputBar);
        setFlexGrow(1, messageArea);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("chat.pageTitle");
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer parameter) {
        if (parameter == null) {
            Notification.show(getTranslation("chat.error.invalidRequest"), 3000, Notification.Position.MIDDLE);
            return;
        }
        this.requestId = parameter;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        currentUser = userRepository.findByEmail(email).orElse(null);

        if (currentUser == null || requestId == null) {
            Notification.show(getTranslation("chat.error.unableToOpen"), 3000, Notification.Position.MIDDLE);
            return;
        }

        currentRequest = requestRepository.findById(requestId).orElse(null);
        if (currentRequest == null) {
            Notification.show(getTranslation("chat.error.notFound"), 3000, Notification.Position.MIDDLE);
            return;
        }

        boolean isOwner = currentRequest.getCustomer() != null
                && currentRequest.getCustomer().getUserId().equals(currentUser.getUserId());
        boolean isStaff = currentUser.getRole() != null
                && (currentUser.getRole().name().equals("HELPDESK")
                    || currentUser.getRole().name().equals("PO")
                    || currentUser.getRole().name().equals("ADMIN"));

        if (!isOwner && !isStaff) {
            Notification.show(getTranslation("chat.error.unauthorized"), 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            otherPartyId = chatService.resolveOtherParty(requestId, currentUser.getUserId()).getUserId();
        } catch (Exception e) {
            Notification.show(e.getMessage(), 4000, Notification.Position.MIDDLE);
            return;
        }

        chatService.markMessagesAsRead(requestId, currentUser.getUserId());

        loadHistory();

        UI ui = attachEvent.getUI();
        registration = ChatBroadcaster.register(requestId, message ->
                ui.access(() -> appendMessage(message))
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (registration != null) {
            registration.remove();
        }
        super.onDetach(detachEvent);
    }

    private void loadHistory() {
        messageArea.removeAll();
        
        if (currentRequest != null) {
            messageArea.add(createRequestDetailCard(currentRequest));
        }

        for (MessageEntity m : chatService.getMessages(requestId)) {
            appendMessage(m);
        }
    }

    private void appendMessage(MessageEntity message) {
        boolean isMine = message.getSender().getUserId().equals(currentUser.getUserId());

        Div bubble = new Div();
        bubble.addClassName("talep-chat-bubble");
        if (isMine) {
            bubble.addClassName("talep-chat-bubble-mine");
        } else {
            bubble.addClassName("talep-chat-bubble-other");
        }

        Span sender = new Span(message.getSender().getNameSurname());
        sender.addClassName("talep-chat-sender");

        Paragraph content = new Paragraph();
        content.addClassName("talep-chat-content");

        if (message.getFileName() != null && message.getFileData() != null) {
            StreamResource res = new StreamResource(message.getFileName(), () -> new ByteArrayInputStream(message.getFileData()));
            Anchor downloadLink = new Anchor(res, "");
            Button downloadBtn = new Button(message.getFileName(), VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            downloadBtn.addClassName("talep-chat-download-btn");
            downloadLink.add(downloadBtn);
            downloadLink.getElement().setAttribute("download", true);
            
            content.add(downloadLink);
        } else {
            content.setText(message.getMessageBody());
        }

        Span time = new Span(message.getSentAt().format(TIME_FORMAT));
        time.addClassName("talep-chat-time");

        bubble.add(sender, content, time);
        messageArea.add(bubble);

        messageArea.getElement().executeJs(
                "this.scrollTop = this.scrollHeight;"
        );
    }

    private VerticalLayout createRequestDetailCard(RequestEntity request) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.addClassName("talep-chat-detail-card");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        
        Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
        infoIcon.setColor("#55f7cf"); 
        infoIcon.setSize("18px");
        
        Span title = new Span(getTranslation("chat.card.subject") + ": " + request.getTitle());
        title.addClassName("talep-chat-detail-title");
        
        header.add(infoIcon, title);

        Span detailContent = new Span(request.getDescription());
        detailContent.addClassName("talep-chat-detail-content");

        card.add(header, detailContent);
        return card;
    }

    private void sendMessage() {
        String text = input.getValue();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (otherPartyId == null) {
            Notification.show(getTranslation("chat.error.otherPartyNotFound"), 3000, Notification.Position.MIDDLE);
            return;
        }
        try {
            chatService.sendMessage(requestId, currentUser.getUserId(), otherPartyId, text);
            
            systemLogService.log("Kullanıcı (" + currentUser.getEmail() + ") ID: " + requestId + " olan talep sohbetine mesaj gönderdi.");

            input.clear();
        } catch (Exception e) {
            Notification.show(getTranslation("chat.error.sendFailed") + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }
}