package com.example.base.ui.MainScreen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.base.entity.GlobalChatMessageEntity;
import com.example.base.entity.NotificationEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.GlobalChatBroadcaster;
import com.example.base.service.GlobalChatService;
import com.example.base.service.NotificationBroadcaster;
import com.example.base.service.NotificationService;

import com.example.base.ui.AdminScreen.AdminDashboardView;
import com.example.base.ui.AdminScreen.AdminSettingsView;
import com.example.base.ui.AdminScreen.AdminUserManagmentView;
import com.example.base.ui.AdminScreen.SystemLogsView;
import com.example.base.ui.AdminScreen.AdminManagment.AdminManagmentView;
import com.example.base.ui.Chat.GenelChatView;
import com.example.base.ui.CustomerScreen.CustomerDashboardView;
import com.example.base.ui.CustomerScreen.FaqView;
import com.example.base.ui.CustomerScreen.TalepAcmaView.TalepAcma;
import com.example.base.ui.CustomerScreen.Taleplerim.TaleplerimView;
import com.example.base.ui.HelpDeskerScreen.HelpdeskDashboardView;
import com.example.base.ui.HelpDeskerScreen.MusteriOnayView;
import com.example.base.ui.HelpDeskerScreen.OnIncelemeView;
import com.example.base.ui.PoScreen.PODashboardView;
import com.example.base.ui.PoScreen.TalepDegerlendirme;
import com.example.base.ui.ProgrammerScreen.ProgrammerTaskView;
import com.example.base.ui.PoScreen.KvkkApprovalView;
import com.example.base.ui.ProgrammerScreen.ProgrammerDashboardView;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationBroadcaster broadcaster;
    
    private final GlobalChatService globalChatService;
    private final GlobalChatBroadcaster globalChatBroadcaster;

    private Integer currentUserId;
    private MenuItem bellItem;
    
    private SideNavItem genelChatNavItem;
    
    private Consumer<Void> broadcastListener;
    private Consumer<GlobalChatMessageEntity> globalChatBroadcastListener;

    public MainLayout(AuthenticationContext authContext, UserRepository userRepository, 
                      NotificationService notificationService, NotificationBroadcaster broadcaster,
                      GlobalChatService globalChatService, GlobalChatBroadcaster globalChatBroadcaster) {
        this.authContext = authContext;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.broadcaster = broadcaster;
        this.globalChatService = globalChatService;
        this.globalChatBroadcaster = globalChatBroadcaster;
        
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        Image logo = new Image("images/logo2.png", "Logo");
        logo.setHeight("50px");

        H1 title = new H1(getTranslation("app.title"));
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        HorizontalLayout brandLayout = new HorizontalLayout(logo, title);
        brandLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brandLayout.getStyle().set("cursor", "pointer"); 
        brandLayout.getElement().setProperty("title", getTranslation("app.home.tooltip"));
        brandLayout.addClickListener(e -> UI.getCurrent().navigate(HomeView.class));

        ComboBox<Locale> languageSelector = new ComboBox<>();
        languageSelector.setItems(new Locale("tr", "TR"), new Locale("en", "US"));
        languageSelector.setItemLabelGenerator(loc -> loc.getLanguage().equals("tr") ? "🇹🇷 TR" : "🇬🇧 EN");
        languageSelector.setValue(UI.getCurrent().getLocale() != null ? UI.getCurrent().getLocale() : new Locale("tr", "TR"));
        languageSelector.setWidth("100px");
        languageSelector.getStyle().set("margin-left", "auto").set("margin-right", "15px");
        
        languageSelector.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().equals(e.getOldValue())) {
                UI.getCurrent().setLocale(e.getValue());
                VaadinSession.getCurrent().setLocale(e.getValue());
                UI.getCurrent().getPage().reload();
            }
        });

        MenuBar userMenu = new MenuBar();
        userMenu.setOpenOnHover(false);

        String email = authContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("Anonim");

        MenuBar notificationMenu = new MenuBar();
        notificationMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
        
        bellItem = notificationMenu.addItem(VaadinIcon.BELL.create());

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            currentUserId = user.getUserId();
            
            updateNotificationsUI();
            updateGlobalChatBadge();

            Avatar avatar = new Avatar();
            avatar.setName(user.getNameSurname());

            String photoUrl = user.getProfilePhotoUrl();
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
            
            MenuItem avatarItem = userMenu.addItem(avatar);
            avatarItem.getSubMenu().addItem(getTranslation("menu.profileSettings"), e -> getUI().ifPresent(ui -> ui.navigate(ProfilView.class)));
            avatarItem.getSubMenu().addItem(getTranslation("menu.logout"), e -> authContext.logout());
            
        } else {
            Avatar avatar = new Avatar();
            avatar.setName(email);
            MenuItem avatarItem = userMenu.addItem(avatar);
            avatarItem.getSubMenu().addItem(getTranslation("menu.logout"), e -> authContext.logout());
        }

        HorizontalLayout header = new HorizontalLayout(toggle, brandLayout, languageSelector, notificationMenu, userMenu);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding-right", "15px");

        addToNavbar(header);
    }

    public void updateGlobalChatBadge() {
        if (currentUserId == null || genelChatNavItem == null) return;
        
        int unreadCount = globalChatService.getUnreadCountForUser(currentUserId);
        
        if (unreadCount > 0) {
            Badge badge = new Badge(String.valueOf(unreadCount));
            badge.addThemeVariants(BadgeVariant.ERROR);
            badge.getStyle().set("font-size", "10px");
            genelChatNavItem.setSuffixComponent(badge);
        } else {
            genelChatNavItem.setSuffixComponent(null);
        }
    }

    private void updateNotificationsUI() {
        if (currentUserId == null) return;
        
        bellItem.removeAll();
        bellItem.getSubMenu().removeAll();
        bellItem.add(VaadinIcon.BELL.create());
        
        List<NotificationEntity> notifs = notificationService.getUnreadNotifications(currentUserId);
        
        if (!notifs.isEmpty()) {
            Span badge = new Span(String.valueOf(notifs.size()));
            badge.getElement().getThemeList().add("badge error primary pill");
            badge.getStyle().set("margin-left", "5px").set("font-size", "10px");
            bellItem.add(badge);

            for (NotificationEntity notif : notifs) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidth("320px");
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                row.getStyle()
                    .set("padding", "6px 8px")
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("transition", "all 0.3s ease");

                Span text = new Span(notif.getTitle() + ": " + notif.getContent());
                text.getStyle()
                    .set("font-size", "13px")
                    .set("white-space", "normal")
                    .set("flex-grow", "1")
                    .set("cursor", "pointer");

                text.addClickListener(e -> {
                    String title = notif.getTitle();
                    if (title != null) {
                        if (title.contains("KVKK")) {
                            UI.getCurrent().navigate(KvkkApprovalView.class);
                        } else if (title.contains("Yeni Talep")) {
                            UI.getCurrent().navigate(TalepDegerlendirme.class);
                        }
                    }
                    notificationService.markAsRead(notif);
                    updateNotificationsUI();
                });

                Button checkBtn = new Button(VaadinIcon.CHECK.create());
                checkBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                checkBtn.getElement().setProperty("title", getTranslation("notification.markAsRead"));
                checkBtn.addClickListener(e -> {
                    notificationService.markAsRead(notif);
                    row.getStyle().set("transform", "translateX(100%)");
                    row.getStyle().set("opacity", "0");
                    row.setVisible(false);
                    updateNotificationsUI(); 
                });
                
                HorizontalLayout buttons = new HorizontalLayout(checkBtn);
                buttons.setSpacing(false);
                buttons.getStyle().set("flex-shrink", "0");

                row.add(text, buttons);

                MenuItem item = bellItem.getSubMenu().addItem(row);
                item.setEnabled(true);
            }
        } else {
            MenuItem emptyItem = bellItem.getSubMenu().addItem(getTranslation("notification.empty"));
            emptyItem.setEnabled(false);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        
        if (currentUserId != null) {
            broadcastListener = (v) -> {
                ui.access(this::updateNotificationsUI);
            };
            broadcaster.register(currentUserId, broadcastListener);

            globalChatBroadcastListener = (msg) -> {
                ui.access(this::updateGlobalChatBadge);
            };
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

    private void createDrawer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            boolean isGod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GODPANEL"));

            if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                SideNav customerNav = new SideNav();
                if (isGod) customerNav.setLabel("Customer");
                customerNav.addItem(new SideNavItem(getTranslation("menu.newRequest"), TalepAcma.class, VaadinIcon.PLUS.create()));
                customerNav.addItem(new SideNavItem(getTranslation("menu.myRequests"), TaleplerimView.class, VaadinIcon.LIST.create()));
                customerNav.addItem(new SideNavItem(getTranslation("menu.dashboard"), CustomerDashboardView.class, VaadinIcon.CHART_LINE.create()));                
                addToDrawer(customerNav);
            }

            if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROGRAMMER"))) {
                SideNav programmerNav = new SideNav();
                if (isGod) programmerNav.setLabel("Programmer");
                programmerNav.addItem(new SideNavItem(getTranslation("menu.programmerTasks"), ProgrammerTaskView.class, VaadinIcon.CODE.create()));
                programmerNav.addItem(new SideNavItem(getTranslation("menu.dashboard"), ProgrammerDashboardView.class, VaadinIcon.CHART_LINE.create()));
                
                genelChatNavItem = new SideNavItem("Genel Sohbet", GenelChatView.class);
                genelChatNavItem.setPrefixComponent(VaadinIcon.COMMENTS.create());
                programmerNav.addItem(genelChatNavItem);
                
                addToDrawer(programmerNav);
            }

            if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                SideNav adminNav = new SideNav();
                if (isGod) adminNav.setLabel("Admin");
                adminNav.addItem(new SideNavItem(getTranslation("menu.userManagement"), AdminUserManagmentView.class, VaadinIcon.USERS.create())); 
                adminNav.addItem(new SideNavItem(getTranslation("menu.systemLogs"), SystemLogsView.class, VaadinIcon.CHART_LINE.create()));
                adminNav.addItem(new SideNavItem(getTranslation("menu.dashboard"), AdminDashboardView.class, VaadinIcon.LIST_SELECT.create()));
                adminNav.addItem(new SideNavItem(getTranslation("menu.systemManagement"), AdminManagmentView.class, VaadinIcon.LIST.create()));
                adminNav.addItem(new SideNavItem(getTranslation("menu.systemSettings"), AdminSettingsView.class, VaadinIcon.COG.create()));
                
                genelChatNavItem = new SideNavItem("Genel Sohbet", GenelChatView.class);
                genelChatNavItem.setPrefixComponent(VaadinIcon.COMMENTS.create());
                adminNav.addItem(genelChatNavItem);
                
                addToDrawer(adminNav);
            }

            if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HELPDESK"))) {
                SideNav helpdeskNav = new SideNav();
                if (isGod) helpdeskNav.setLabel("Helpdesk");
                helpdeskNav.addItem(new SideNavItem(getTranslation("menu.incomingRequests"), OnIncelemeView.class, VaadinIcon.INBOX.create())); 
                helpdeskNav.addItem(new SideNavItem(getTranslation("menu.customerApprovals"), MusteriOnayView.class, VaadinIcon.USER_CHECK.create()));
                helpdeskNav.addItem(new SideNavItem(getTranslation("menu.performanceReport"), HelpdeskDashboardView.class, VaadinIcon.CHART_LINE.create())); 
                
                genelChatNavItem = new SideNavItem("Genel Sohbet", GenelChatView.class);
                genelChatNavItem.setPrefixComponent(VaadinIcon.COMMENTS.create());
                helpdeskNav.addItem(genelChatNavItem);
                
                addToDrawer(helpdeskNav);
            }

            if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
                SideNav poNav = new SideNav();
                if (isGod) poNav.setLabel("PO");
                poNav.addItem(new SideNavItem(getTranslation("menu.pendingRequests"), TalepDegerlendirme.class, VaadinIcon.LIST_SELECT.create()));
                poNav.addItem(new SideNavItem(getTranslation("menu.dashboard"), PODashboardView.class, VaadinIcon.CHART_LINE.create()));
                poNav.addItem(new SideNavItem(getTranslation("menu.kvkkRequests"), KvkkApprovalView.class, VaadinIcon.NOTEBOOK.create()));
                
                genelChatNavItem = new SideNavItem("Genel Sohbet", GenelChatView.class);
                genelChatNavItem.setPrefixComponent(VaadinIcon.COMMENTS.create());
                poNav.addItem(genelChatNavItem);
                
                addToDrawer(poNav);
            }
        }
    }
}