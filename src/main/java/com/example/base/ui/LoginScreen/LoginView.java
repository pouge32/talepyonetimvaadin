package com.example.base.ui.LoginScreen;

import java.util.List;
import java.util.Locale;

import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "login", autoLayout = false)
@AnonymousAllowed
public class LoginView extends Main implements BeforeEnterObserver, HasDynamicTitle {

    private final LoginForm login;
    private final SystemLogService systemLogService;

    public LoginView(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
        
        // Oturumda kayıtlı bir dil tercihi varsa uygula
        Locale sessionLocale = (Locale) VaadinSession.getCurrent().getAttribute("session_locale");
        if (sessionLocale != null) {
            UI.getCurrent().setLocale(sessionLocale);
        }

        login = new LoginForm();
        login.setAction("login");

        login.getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.innerHTML = '[part=\"error-message\"] { display: none !important; }';" +
            "this.shadowRoot.appendChild(style);"
        );

        LoginI18n i18n = LoginI18n.createDefault();
        
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle(getTranslation("login.title"));
        i18nForm.setUsername(getTranslation("login.username"));
        i18nForm.setPassword(getTranslation("login.password"));
        i18nForm.setSubmit(getTranslation("login.submit"));
        i18nForm.setForgotPassword(getTranslation("login.forgotPassword"));
        i18n.setForm(i18nForm);
        
        login.setI18n(i18n);

        login.addForgotPasswordListener(e -> getUI().ifPresent(ui -> ui.navigate("sifremi-unuttum")));

        Image logo = new Image("images/logo.png", "Logo");
        logo.setHeight("200px");
        logo.getStyle().set("margin-bottom", "10px");

        Button registerButton = new Button(getTranslation("login.registerButton"));
        registerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        registerButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("kayit-ol")));

        ComboBox<String> languageSelect = new ComboBox<>();
        languageSelect.setItems("TR", "EN");
        
        Locale currentLocale = UI.getCurrent().getLocale();
        boolean isEn = currentLocale != null && currentLocale.getLanguage().equalsIgnoreCase("en");
        languageSelect.setValue(isEn ? "EN" : "TR");
        
        languageSelect.setWidth("90px");
        languageSelect.addValueChangeListener(event -> {
            if (event.getValue() != null && event.isFromClient()) {
                String selectedLang = event.getValue();
                Locale newLocale = selectedLang.equals("EN") ? Locale.ENGLISH : new Locale("tr", "TR");
                
                VaadinSession.getCurrent().setAttribute("session_locale", newLocale);
                UI.getCurrent().setLocale(newLocale);
                UI.getCurrent().getPage().reload();
            }
        });

        Div langContainer = new Div(languageSelect);
        langContainer.getStyle()
                .set("position", "absolute")
                .set("top", "20px")
                .set("right", "20px");

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        layout.add(logo, login, registerButton);
        layout.setSizeFull();

        add(langContainer, layout);
        setSizeFull();
        getStyle().set("position", "relative");
    }

    @Override
    public String getPageTitle() {
        return getTranslation("login.pageTitle");
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
            login.setError(false);
            String errorMessage = switch (errorParam) {
                case "banned" -> getTranslation("login.error.banned");
                case "pending" -> getTranslation("login.error.pending");
                case "rejected" -> getTranslation("login.error.rejected");
                case "locked" -> getTranslation("login.error.locked");
                default -> getTranslation("login.error.default");
            };

            Notification.show(errorMessage, 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);

            systemLogService.log(getTranslation("login.log.failedAttempt") + " " + errorMessage);
        }
    }
}