package com.example.base.ui.MainScreen;

import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.base.entity.GlobalChatMessageEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.GlobalChatBroadcaster;
import com.example.base.service.GlobalChatService;
import com.example.base.service.NotificationBroadcaster;
import com.example.base.service.NotificationService;
import com.example.base.ui.MainScreen.MainLayoutView.MainLayoutDrawer;
import com.example.base.ui.MainScreen.MainLayoutView.MainLayoutHeader;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;

@PermitAll
@CssImport("./styles/main/main-layout.css")
public class MainLayout extends AppLayout {

    private final NotificationService notificationService;
    private final NotificationBroadcaster broadcaster;
    private final GlobalChatService globalChatService;
    private final GlobalChatBroadcaster globalChatBroadcaster;

    private Integer currentUserId;
    private final MainLayoutHeader header;
    private final MainLayoutDrawer drawerHelper;

    private Consumer<Void> broadcastListener;
    private Consumer<GlobalChatMessageEntity> globalChatBroadcastListener;

    public MainLayout(AuthenticationContext authContext, UserRepository userRepository,
                      NotificationService notificationService, NotificationBroadcaster broadcaster,
                      GlobalChatService globalChatService, GlobalChatBroadcaster globalChatBroadcaster) {
        this.notificationService = notificationService;
        this.broadcaster = broadcaster;
        this.globalChatService = globalChatService;
        this.globalChatBroadcaster = globalChatBroadcaster;

        this.drawerHelper = new MainLayoutDrawer(this);
        this.header = new MainLayoutHeader(authContext, userRepository, notificationService, () -> {
            String email = authContext.getAuthenticatedUser(UserDetails.class)
                    .map(UserDetails::getUsername)
                    .orElse("");
            userRepository.findByEmail(email).ifPresent(user -> this.currentUserId = user.getUserId());
        });

        addToNavbar(header);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        drawerHelper.buildDrawer(auth, this::addToDrawer);

        updateNotificationsUI();
        updateGlobalChatBadge();
    }

    public void updateGlobalChatBadge() {
        drawerHelper.updateGlobalChatBadge(currentUserId, globalChatService);
    }

    private void updateNotificationsUI() {
        header.getNotificationMenuHelper().updateNotificationsUI(currentUserId);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        if (currentUserId != null) {
            broadcastListener = (v) -> ui.access(this::updateNotificationsUI);
            broadcaster.register(currentUserId, broadcastListener);

            globalChatBroadcastListener = (msg) -> ui.access(this::updateGlobalChatBadge);
            globalChatBroadcaster.register(globalChatBroadcastListener);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (currentUserId != null) {
            if (broadcastListener != null) broadcaster.unregister(currentUserId, broadcastListener);
            if (globalChatBroadcastListener != null) globalChatBroadcaster.unregister(globalChatBroadcastListener);
        }
        super.onDetach(detachEvent);
    }
}