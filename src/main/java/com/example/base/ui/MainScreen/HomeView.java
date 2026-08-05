package com.example.base.ui.MainScreen;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class) 
@PermitAll 
public class HomeView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;

    private final TextField searchBox = new TextField();
    private final Div resultsContainer = new Div();
    
    private final Grid<RequestEntity> requestGrid = new Grid<>(RequestEntity.class, false);
    private final Grid<UserEntity> userGrid = new Grid<>(UserEntity.class, false);
    
    private final Div requestSection = new Div();
    private final Div userSection = new Div();
    
    private final Span noResultMsg = new Span();
    
    private final H3 requestSectionTitle = new H3();
    private final H3 userSectionTitle = new H3();

    public HomeView(RequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "40px")
                .set("max-width", "1000px")
                .set("margin", "0 auto")
                .set("min-height", "600px");

        H2 title = new H2(getTranslation("home.welcomeTitle"));
        title.getStyle().set("margin-top", "0").set("text-align", "center");
        Paragraph subtitle = new Paragraph(getTranslation("home.welcomeSubtitle"));
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("text-align", "center").set("margin-bottom", "30px");

        HorizontalLayout searchBarLayout = new HorizontalLayout();
        searchBarLayout.setWidthFull();
        searchBarLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        searchBox.setPlaceholder(getTranslation("home.searchPlaceholder"));
        searchBox.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchBox.setWidthFull();
        searchBox.setClearButtonVisible(true);
        searchBox.setValueChangeMode(ValueChangeMode.LAZY);
        searchBox.setValueChangeTimeout(800); 
        searchBox.addValueChangeListener(e -> performSearch(e.getValue()));

        searchBarLayout.add(searchBox);
        searchBarLayout.expand(searchBox);

        configureGrids();

        requestSectionTitle.setText(getTranslation("home.foundRequests"));
        userSectionTitle.setText(getTranslation("home.foundUsers"));

        requestSection.add(requestSectionTitle, requestGrid);
        userSection.add(userSectionTitle, userGrid);
        
        requestSection.setVisible(false);
        userSection.setVisible(false);

        noResultMsg.getStyle().set("color", "var(--lumo-error-text-color)")
                   .set("font-weight", "bold")
                   .set("display", "block")
                   .set("text-align", "center")
                   .set("padding", "20px");
        noResultMsg.setVisible(false);

        resultsContainer.setWidthFull();
        resultsContainer.getStyle().set("margin-top", "30px");
        resultsContainer.add(noResultMsg, requestSection, userSection);

        mainContainer.add(title, subtitle, searchBarLayout, resultsContainer);
        add(mainContainer);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("home.pageTitle");
    }

    private void configureGrids() {
        requestGrid.addColumn(RequestEntity::getRequestId).setHeader(getTranslation("home.grid.id")).setAutoWidth(true).setFlexGrow(0);
        requestGrid.addColumn(RequestEntity::getTitle).setHeader(getTranslation("home.grid.title")).setFlexGrow(1);
        
        requestGrid.addColumn(req -> {
            try {
                return req.getCustomer() != null ? req.getCustomer().getNameSurname() + " (" + req.getCustomer().getEmail() + ")" : "-";
            } catch (Exception e) {
                return getTranslation("home.unknown");
            }
        }).setHeader(getTranslation("home.grid.customer")).setAutoWidth(true);
        
        requestGrid.addComponentColumn(this::createStatusBadge).setHeader(getTranslation("home.grid.status")).setAutoWidth(true).setFlexGrow(0);
        requestGrid.addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-").setHeader(getTranslation("home.grid.date")).setAutoWidth(true);
        requestGrid.setHeight("250px");

        userGrid.addColumn(UserEntity::getUserId).setHeader(getTranslation("home.grid.id")).setAutoWidth(true).setFlexGrow(0);
        userGrid.addColumn(UserEntity::getNameSurname).setHeader(getTranslation("home.grid.nameSurname")).setFlexGrow(1);
        userGrid.addColumn(UserEntity::getEmail).setHeader(getTranslation("home.grid.email")).setFlexGrow(1);
        userGrid.addComponentColumn(this::createRoleBadge).setHeader(getTranslation("home.grid.role")).setAutoWidth(true).setFlexGrow(0);
        userGrid.setHeight("200px");
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase(new Locale("tr", "TR"))
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c");
    }

    private void performSearch(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            requestSection.setVisible(false);
            userSection.setVisible(false);
            noResultMsg.setVisible(false);
            return;
        }

        String kw = normalizeText(keyword.trim());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();
        boolean isCustomer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("CUSTOMER"));

        UserEntity currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        final Integer currentUserId = (currentUser != null) ? currentUser.getUserId() : -1;

        try {
            List<RequestEntity> matchedRequests = requestRepository.findAll().stream()
                    .filter(req -> {
                        boolean isOwner = false;
                        String customerEmail = "";
                        String customerName = "";

                        try {
                            if (req.getCustomer() != null) {
                                if (req.getCustomer().getUserId().equals(currentUserId)) {
                                    isOwner = true;
                                }
                                customerEmail = normalizeText(req.getCustomer().getEmail());
                                customerName = normalizeText(req.getCustomer().getNameSurname());
                            }
                        } catch (Exception ignored) {}

                        if (isCustomer && !isOwner) {
                            return false;
                        }

                        String idStr = String.valueOf(req.getRequestId());
                        String titleStr = normalizeText(req.getTitle());
                        String descStr = normalizeText(req.getDescription());

                        return idStr.equals(kw) || titleStr.contains(kw) || descStr.contains(kw) || 
                               customerEmail.contains(kw) || customerName.contains(kw);
                    })
                    .collect(Collectors.toList());

            requestGrid.setItems(matchedRequests);
            requestSection.setVisible(!matchedRequests.isEmpty());

            List<UserEntity> matchedUsers = List.of();
            if (!isCustomer) {
                matchedUsers = userRepository.findAll().stream()
                        .filter(user -> {
                            String idStr = String.valueOf(user.getUserId());
                            String emailStr = normalizeText(user.getEmail());
                            String roleStr = user.getRole() != null ? normalizeText(user.getRole().name()) : "";
                            String nameStr = normalizeText(user.getNameSurname());

                            return idStr.equals(kw) || emailStr.contains(kw) || roleStr.contains(kw) || nameStr.contains(kw);
                        })
                        .collect(Collectors.toList());

                userGrid.setItems(matchedUsers);
                userSection.setVisible(!matchedUsers.isEmpty());
            } else {
                userSection.setVisible(false);
            }

            if (matchedRequests.isEmpty() && matchedUsers.isEmpty()) {
                noResultMsg.setText(getTranslation("home.noResultBefore") + " '" + keyword + "' " + getTranslation("home.noResultAfter"));
                noResultMsg.setVisible(true);
            } else {
                noResultMsg.setVisible(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(status);
        switch (status) {
            case "NEW": badge.addThemeVariants(BadgeVariant.CONTRAST); break;
            case "INCELEMEDE": badge.addThemeVariants(BadgeVariant.WARNING); break;
            case "ONAYLANDI": 
            case "İş Akışına Dönüştü": badge.addThemeVariants(BadgeVariant.SUCCESS); break;
            case "KAPATILDI": badge.addThemeVariants(BadgeVariant.ERROR); break;
            default: badge.addThemeVariants(BadgeVariant.CONTRAST); break;
        }
        return badge;
    }

    private Component createRoleBadge(UserEntity user) {
        String role = user.getRole() != null ? user.getRole().name() : "BİLİNMİYOR";
        Badge badge = new Badge(role);
        
        if (role.contains("ADMIN")) {
        } else if (role.contains("PO") || role.contains("PROGRAMMER")) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        } else {
            badge.addThemeVariants(BadgeVariant.CONTRAST);
        }
        return badge;
    }
}