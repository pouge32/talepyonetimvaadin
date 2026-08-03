package com.example.base.ui.LoginScreen;

import java.util.List;

import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "login", autoLayout = false)
@PageTitle("Giriş Yap")
@AnonymousAllowed
public class LoginView extends Main implements BeforeEnterObserver {

    private final LoginForm login;
    private final SystemLogService systemLogService;

    public LoginView(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
        login = new LoginForm();
        login.setAction("login");

        Image logo = new Image("images/logo.png", "Logo");
        logo.setHeight("200px");
        logo.getStyle().set("margin-bottom", "10px");

        Button registerButton = new Button("Yeni Müşteri Kaydı Oluştur");
        registerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        registerButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("kayit-ol")));

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        layout.add(logo, login, registerButton);
        layout.setSizeFull();

        add(layout);
        setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String errorParam = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .getOrDefault("error", List.of())
                .stream()
                .findFirst()
                .orElse(null);
        
        if (errorParam != null) {
            login.setError(true);
            String errorMessage = switch (errorParam) {
                case "banned" -> "Bu hesap yasaklanmıştır.";
                case "pending" -> "Hesabınız henüz destek ekibi tarafından onaylanmadı!";
                case "rejected" -> "Kayıt başvurunuz reddedilmiştir.";
                default -> "Hatalı e-posta veya şifre!";
            };

            Notification.show(errorMessage, 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);

            systemLogService.log("Başarısız giriş denemesi: " + errorMessage);
        }
    }
}