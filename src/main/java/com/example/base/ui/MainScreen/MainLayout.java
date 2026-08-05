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

import com.example.base.entity.NotificationEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.NotificationBroadcaster;
import com.example.base.service.NotificationService;

import com.example.base.ui.AdminScreen.AdminDashboardView;
import com.example.base.ui.AdminScreen.AdminSettingsView;
import com.example.base.ui.AdminScreen.AdminUserManagmentView;
import com.example.base.ui.AdminScreen.SystemLogsView;
import com.example.base.ui.CustomerScreen.CustomerDashboardView;
import com.example.base.ui.CustomerScreen.TalepAcma;
import com.example.base.ui.CustomerScreen.TaleplerimView;
import com.example.base.ui.HelpDeskerScreen.HelpdeskDashboardView;
import com.example.base.ui.HelpDeskerScreen.MusteriOnayView;
import com.example.base.ui.HelpDeskerScreen.OnIncelemeView;
import com.example.base.ui.PoScreen.PODashboardView;
import com.example.base.ui.PoScreen.TalepDegerlendirme;
import com.example.base.ui.ProgrammerScreen.ProgrammerDashboardView;
import com.example.base.ui.AdminScreen.AdminManagmentView;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
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

    private Integer currentUserId;
    private MenuItem bellItem;
    private Consumer<Void> broadcastListener;

    public MainLayout(AuthenticationContext authContext, UserRepository userRepository, 
                      NotificationService notificationService, NotificationBroadcaster broadcaster) {
        this.authContext = authContext;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.broadcaster = broadcaster;
        
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
                    .set("flex-grow", "1");

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
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (currentUserId != null && broadcastListener != null) {
            broadcaster.unregister(currentUserId, broadcastListener);
        }
        super.onDetach(detachEvent);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                nav.addItem(new SideNavItem(getTranslation("menu.newRequest"), TalepAcma.class, VaadinIcon.PLUS_CIRCLE.create()));
                nav.addItem(new SideNavItem(getTranslation("menu.myRequests"), TaleplerimView.class, VaadinIcon.LIST.create())); 
                nav.addItem(new SideNavItem(getTranslation("menu.dashboard"), CustomerDashboardView.class, VaadinIcon.CHART_LINE.create()));
            }
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
                nav.addItem(new SideNavItem(getTranslation("menu.pendingRequests"), TalepDegerlendirme.class, VaadinIcon.LIST_SELECT.create()));
                nav.addItem(new SideNavItem(getTranslation("menu.dashboard"), PODashboardView.class, VaadinIcon.CHART_LINE.create())); 
            }
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HELPDESK"))) {
                nav.addItem(new SideNavItem(getTranslation("menu.incomingRequests"), OnIncelemeView.class, VaadinIcon.INBOX.create())); 
                nav.addItem(new SideNavItem(getTranslation("menu.customerApprovals"), MusteriOnayView.class, VaadinIcon.USER_CHECK.create()));
                nav.addItem(new SideNavItem(getTranslation("menu.performanceReport"), HelpdeskDashboardView.class, VaadinIcon.CHART_LINE.create())); 
            }
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                nav.addItem(new SideNavItem(getTranslation("menu.userManagement"), AdminUserManagmentView.class, VaadinIcon.USERS.create())); 
                nav.addItem(new SideNavItem(getTranslation("menu.systemLogs"), SystemLogsView.class, VaadinIcon.CHART_LINE.create()));
                nav.addItem(new SideNavItem(getTranslation("menu.dashboard"), AdminDashboardView.class, VaadinIcon.LIST_SELECT.create()));
                nav.addItem(new SideNavItem(getTranslation("menu.systemManagement"), AdminManagmentView.class, VaadinIcon.LIST.create()));
                nav.addItem(new SideNavItem(getTranslation("menu.systemSettings"), AdminSettingsView.class, VaadinIcon.COG.create()));
            }
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROGRAMMER"))) {
                nav.addItem(new SideNavItem(getTranslation("menu.programmerTasks"), ProgrammerDashboardView.class, VaadinIcon.CODE.create()));
            }
        }
        addToDrawer(nav);
    }
}