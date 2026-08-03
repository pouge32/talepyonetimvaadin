package com.example.base.ui.MainScreen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.base.entity.NotificationEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.NotificationService;
import com.example.base.ui.AdminScreen.AdminDashboardView;
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
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
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MainLayout(AuthenticationContext authContext, UserRepository userRepository, NotificationService notificationService) {
        this.authContext = authContext;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        Image logo = new Image("images/logo2.png", "Logo");
        logo.setHeight("50px");
        logo.getStyle().set("margin-right", "10px");

        H1 title = new H1("Talep Yönetim Sistemi");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        MenuBar userMenu = new MenuBar();
        userMenu.setOpenOnHover(false);

        String email = authContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("Anonim");

        MenuBar notificationMenu = new MenuBar();
        notificationMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
        MenuItem bellItem = notificationMenu.addItem(VaadinIcon.BELL.create());

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();

            List<NotificationEntity> unreadNotifs = notificationService.getUnreadNotifications(user.getUserId());
            
            if (!unreadNotifs.isEmpty()) {
                Span badge = new Span(String.valueOf(unreadNotifs.size()));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.getStyle().set("margin-left", "5px").set("font-size", "10px");
                bellItem.add(badge);
                
                for (NotificationEntity notif : unreadNotifs) {
                    MenuItem item = bellItem.getSubMenu().addItem(notif.getTitle() + ": " + notif.getContent(), e -> {
                        notificationService.markAsRead(notif);
                        UI.getCurrent().getPage().reload();
                    });
                    item.getStyle().set("font-size", "14px").set("max-width", "300px").set("white-space", "normal");
                }
            } else {
                MenuItem emptyItem = bellItem.getSubMenu().addItem("Yeni bildiriminiz yok.");
                emptyItem.setEnabled(false);
            }

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
            avatarItem.getSubMenu().addItem("Profil Ayarları", e -> {
                getUI().ifPresent(ui -> ui.navigate(ProfilView.class));
            });
            avatarItem.getSubMenu().addItem("Çıkış Yap", e -> authContext.logout());
            
        } else {
            Avatar avatar = new Avatar();
            avatar.setName(email);
            MenuItem avatarItem = userMenu.addItem(avatar);
            avatarItem.getSubMenu().addItem("Çıkış Yap", e -> authContext.logout());
        }

        // HorizontalLayout içerisine 'logo' dahil edildi
        HorizontalLayout header = new HorizontalLayout(toggle, logo, title, notificationMenu, userMenu);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.getStyle().set("padding-right", "15px");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                nav.addItem(new SideNavItem("Yeni Talep Oluştur", TalepAcma.class, VaadinIcon.PLUS_CIRCLE.create()));
                nav.addItem(new SideNavItem("Taleplerim", TaleplerimView.class, VaadinIcon.LIST.create())); 
                nav.addItem(new SideNavItem("Profil Ayarları", ProfilView.class, VaadinIcon.USER.create()));
                nav.addItem(new SideNavItem("Dashboard", CustomerDashboardView.class, VaadinIcon.CHART_LINE.create()));
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
                nav.addItem(new SideNavItem("Bekleyen Talepler", TalepDegerlendirme.class, VaadinIcon.LIST_SELECT.create()));
                nav.addItem(new SideNavItem("Dashboard", PODashboardView.class, VaadinIcon.CHART_LINE.create())); 
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HELPDESK"))) {
                nav.addItem(new SideNavItem("Gelen Talepler (Ön İnceleme)", OnIncelemeView.class, VaadinIcon.INBOX.create())); 
                nav.addItem(new SideNavItem("Müşteri Kayıt Onayları", MusteriOnayView.class, VaadinIcon.USER_CHECK.create()));
                nav.addItem(new SideNavItem("Performans Raporu", HelpdeskDashboardView.class, VaadinIcon.CHART_LINE.create())); 
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                nav.addItem(new SideNavItem("Kullanıcı Yönetimi", AdminUserManagmentView.class, VaadinIcon.USERS.create())); 
                nav.addItem(new SideNavItem("Sistem Logları", SystemLogsView.class, VaadinIcon.CHART_LINE.create()));
                nav.addItem(new SideNavItem("Dashboard", AdminDashboardView.class, VaadinIcon.LIST_SELECT.create()));
                nav.addItem(new SideNavItem("Sistem Yönetimi", AdminManagmentView.class, VaadinIcon.LIST.create()));
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROGRAMMER"))) {
                nav.addItem(new SideNavItem("Programmer Görev Paneli", ProgrammerDashboardView.class, VaadinIcon.CODE.create()));
            }
        }

        addToDrawer(nav);
    }
}