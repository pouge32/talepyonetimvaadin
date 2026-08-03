package com.example.base.ui.Class;

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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-chat", layout = MainLayout.class)
@RolesAllowed({"CUSTOMER", "HELPDESK", "PO", "ADMIN"})
public class TalepChat extends VerticalLayout implements HasUrlParameter<Integer> {

    private final ChatService chatService;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService; 

    private final VerticalLayout messageArea = new VerticalLayout();
    private final TextField input = new TextField();
    private final Button sendButton = new Button("Gönder");

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
        add(new H3("Canlı Destek Sohbeti"));

        messageArea.setSizeFull();
        messageArea.getStyle()
                .set("overflow-y", "auto")
                .set("border-radius", "8px")
                .set("padding", "12px");

        input.setPlaceholder("Mesajınızı yazın...");
        input.setWidthFull();
        input.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> sendMessage());

        sendButton.addClickListener(e -> sendMessage());

        HorizontalLayout inputBar = new HorizontalLayout(input, sendButton);
        inputBar.setWidthFull();
        inputBar.setFlexGrow(1, input);

        add(messageArea, inputBar);
        setFlexGrow(1, messageArea);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer parameter) {
        if (parameter == null) {
            Notification.show("Geçersiz talep.", 3000, Notification.Position.MIDDLE);
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
            Notification.show("Sohbet açılamadı.", 3000, Notification.Position.MIDDLE);
            return;
        }

        currentRequest = requestRepository.findById(requestId).orElse(null);
        if (currentRequest == null) {
            Notification.show("Talep bulunamadı.", 3000, Notification.Position.MIDDLE);
            return;
        }

        boolean isOwner = currentRequest.getCustomer() != null
                && currentRequest.getCustomer().getUserId().equals(currentUser.getUserId());
        boolean isStaff = currentUser.getRole() != null
                && (currentUser.getRole().name().equals("HELPDESK")
                    || currentUser.getRole().name().equals("PO")
                    || currentUser.getRole().name().equals("ADMIN"));

        if (!isOwner && !isStaff) {
            Notification.show("Bu sohbete erişim yetkiniz yok.", 3000, Notification.Position.MIDDLE);
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
        bubble.getStyle()
                .set("max-width", "70%")
                .set("margin-bottom", "8px")
                .set("padding", "8px 12px")
                .set("border-radius", "10px")
                .set("align-self", isMine ? "flex-end" : "flex-start")
                .set("background", isMine ? "var(--lumo-primary-color-50pct)" : "var(--lumo-contrast-10pct)");

        Span sender = new Span(message.getSender().getNameSurname());
        sender.getStyle().set("font-weight", "bold").set("font-size", "0.8em").set("display", "block");

        Paragraph content = new Paragraph(message.getMessageBody());
        content.getStyle().set("margin", "4px 0");

        Span time = new Span(message.getSentAt().format(TIME_FORMAT));
        time.getStyle().set("font-size", "0.7em").set("color", "var(--lumo-secondary-text-color)");

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
        
        card.getStyle()
            .set("background-color", "#030304") 
            .set("border-radius", "8px")
            .set("padding", "12px")
            .set("margin-bottom", "16px")
            .set("width", "90%") 
            .set("align-self", "center") 
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.2)");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        
        Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
        infoIcon.setColor("#55f7cf"); 
        infoIcon.setSize("18px");
        
        Span title = new Span("Talep Konusu: " + request.getTitle());
        title.getStyle()
            .set("color", "#E2E8F0")
            .set("font-weight", "bold")
            .set("font-size", "14px");
        
        header.add(infoIcon, title);

        Span content = new Span(request.getDescription());
        content.getStyle()
            .set("color", "#CBD5E1")
            .set("font-size", "13px")
            .set("margin-top", "8px")
            .set("line-height", "1.5");

        card.add(header, content);
        return card;
    }

    private void sendMessage() {
        String text = input.getValue();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (otherPartyId == null) {
            Notification.show("Karşı taraf belirlenemedi.", 3000, Notification.Position.MIDDLE);
            return;
        }
        try {
            chatService.sendMessage(requestId, currentUser.getUserId(), otherPartyId, text);
            
            systemLogService.log("Kullanıcı (" + currentUser.getEmail() + ") ID: " + requestId + " olan talep sohbetine mesaj gönderdi.");

            input.clear();
        } catch (Exception e) {
            Notification.show("Mesaj gönderilemedi: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }
}