package com.example.base.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
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

    public MainLayout(AuthenticationContext authContext, UserRepository userRepository) {
        this.authContext = authContext;
        this.userRepository = userRepository;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Talep Yönetim Sistemi");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        MenuBar userMenu = new MenuBar();
        userMenu.setOpenOnHover(false);

        String email = authContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("Anonim");

        Avatar avatar = new Avatar();
        
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
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
        } else {
            avatar.setName(email);
        }

        MenuItem avatarItem = userMenu.addItem(avatar);

        avatarItem.getSubMenu().addItem("Profil Ayarları", e -> {
            getUI().ifPresent(ui -> ui.navigate(ProfilView.class));
        });
        
        avatarItem.getSubMenu().addItem("Çıkış Yap", e -> authContext.logout());

        HorizontalLayout header = new HorizontalLayout(toggle, title, userMenu);
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
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
                nav.addItem(new SideNavItem("Bekleyen Talepler", TalepDegerlendirme.class, VaadinIcon.LIST_SELECT.create()));
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HELPDESK"))) {
                nav.addItem(new SideNavItem("Gelen Talepler (Ön İnceleme)", HomeView.class, VaadinIcon.INBOX.create())); 
                nav.addItem(new SideNavItem("Müşteri Kayıt Onayları", HomeView.class, VaadinIcon.USER_CHECK.create()));
            }

            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                nav.addItem(new SideNavItem("Kullanıcı Yönetimi", HomeView.class, VaadinIcon.USERS.create())); 
                nav.addItem(new SideNavItem("Sistem Logları", HomeView.class, VaadinIcon.CHART_LINE.create()));
                nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.LIST_SELECT.create()));
            }
        }

        addToDrawer(nav);
    }
}