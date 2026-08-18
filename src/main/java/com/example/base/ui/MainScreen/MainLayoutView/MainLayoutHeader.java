package com.example.base.ui.MainScreen.MainLayoutView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.NotificationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.example.base.ui.MainScreen.HomeView;
import com.example.base.ui.MainScreen.ProfilScreen.ProfilView;

public class MainLayoutHeader extends HorizontalLayout {

    private final MenuItem bellItem;
    private final NotificationMenuHelper notificationMenuHelper;

    public MainLayoutHeader(AuthenticationContext authContext, UserRepository userRepository,
                            NotificationService notificationService, Runnable onUserLoaded) {
        setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        setWidthFull();
        addClassName("main-layout-header");

        DrawerToggle toggle = new DrawerToggle();

        Image logo = new Image("images/logo2.png", "Logo");
        logo.setHeight("50px");

        H1 title = new H1(getTranslation("app.title"));
        title.addClassName("main-layout-title");

        HorizontalLayout brandLayout = new HorizontalLayout(logo, title);
        brandLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brandLayout.addClassName("main-layout-brand");
        brandLayout.getElement().setProperty("title", getTranslation("app.home.tooltip"));
        brandLayout.addClickListener(e -> UI.getCurrent().navigate(HomeView.class));

        ComboBox<Locale> languageSelector = new ComboBox<>();
        languageSelector.setItems(new Locale("tr", "TR"), new Locale("en", "US"));
        languageSelector.setItemLabelGenerator(loc -> loc.getLanguage().equals("tr") ? "🇹🇷 TR" : "🇬🇧 EN");
        languageSelector.setValue(UI.getCurrent().getLocale() != null ? UI.getCurrent().getLocale() : new Locale("tr", "TR"));
        languageSelector.setWidth("100px");
        languageSelector.addClassName("main-layout-lang-selector");

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
        this.notificationMenuHelper = new NotificationMenuHelper(bellItem, notificationService);

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();

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
                        } catch (FileNotFoundException ex) {
                            return new ByteArrayInputStream(new byte[0]);
                        }
                    });
                    avatar.setImageResource(resource);
                }
            }

            MenuItem avatarItem = userMenu.addItem(avatar);
            avatarItem.getSubMenu().addItem(getTranslation("menu.profileSettings"), e -> UI.getCurrent().navigate(ProfilView.class));
            avatarItem.getSubMenu().addItem(getTranslation("menu.logout"), e -> authContext.logout());

            if (onUserLoaded != null) onUserLoaded.run();

        } else {
            Avatar avatar = new Avatar();
            avatar.setName(email);
            MenuItem avatarItem = userMenu.addItem(avatar);
            avatarItem.getSubMenu().addItem(getTranslation("menu.logout"), e -> authContext.logout());
        }

        add(toggle, brandLayout, languageSelector, notificationMenu, userMenu);
    }

    public NotificationMenuHelper getNotificationMenuHelper() {
        return notificationMenuHelper;
    }
}