package com.example.base.ui.LoginScreen;

import java.util.Optional;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("sifremi-unuttum")
@AnonymousAllowed 
public class SifremiUnuttumView extends VerticalLayout implements HasDynamicTitle {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SifremiUnuttumView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle().set("background-color", "var(--lumo-base-color)"); 

        Image logo = new Image("images/logo2.png", "Monad Logo");
        logo.setMaxWidth("150px");

        Div card = new Div();
        card.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("padding", "40px")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 15px rgba(0, 0, 0, 0.2)")
            .set("width", "100%")
            .set("max-width", "400px")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("align-items", "center");

        H2 title = new H2(getTranslation("forgotPassword.title"));
        title.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(getTranslation("forgotPassword.description"));
        description.getStyle().set("text-align", "center").set("color", "var(--lumo-secondary-text-color)");

        EmailField emailField = new EmailField(getTranslation("forgotPassword.email"));
        emailField.setWidthFull();
        emailField.setPlaceholder(getTranslation("forgotPassword.emailPlaceholder"));
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setRequiredIndicatorVisible(true);

        Button submitButton = new Button(getTranslation("forgotPassword.submit"));
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();
        submitButton.getStyle().set("margin-top", "20px");

        Button backToLoginBtn = new Button(getTranslation("forgotPassword.backToLogin"), VaadinIcon.ARROW_LEFT.create());
        backToLoginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backToLoginBtn.getStyle().set("margin-top", "10px");
        backToLoginBtn.addClickListener(e -> UI.getCurrent().navigate("login")); 

        submitButton.addClickListener(e -> {
            if (emailField.isEmpty() || emailField.isInvalid()) {
                Notification.show(getTranslation("forgotPassword.error.invalidEmail"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            processPasswordReset(emailField.getValue());
        });

        card.add(logo, title, description, emailField, submitButton, backToLoginBtn);
        add(card);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("forgotPassword.pageTitle");
    }

    private void processPasswordReset(String email) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            
            String tempPassword = String.format("%08d", new Random().nextInt(99999999));
            
            String encodedPassword = passwordEncoder.encode(tempPassword);
            user.setPasswordHash(encodedPassword);
            user.setPassword(tempPassword);
            
            userRepository.save(user);

            mockSendResetEmail(email, tempPassword);
        }

        Notification.show(getTranslation("forgotPassword.success"), 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        
        UI.getCurrent().navigate("login");
    }

    private void mockSendResetEmail(String to, String tempPassword) {
        System.out.println("\n========================================================");
        System.out.println("  [MOCK MAİL SERVİSİ] - ŞİFRE SIFIRLAMA");
        System.out.println("========================================================");
        System.out.println("  Alıcı E-Posta : " + to);
        System.out.println("  Mesaj         : Şifre sıfırlama talebiniz alınmıştır.");
        System.out.println("  Geçici Şifre  : " + tempPassword);
        System.out.println("  Lütfen giriş yaptıktan sonra Profil ayarlarınızdan");
        System.out.println("  şifrenizi değiştirmeyi unutmayın.");
        System.out.println("========================================================\n");
    }
}