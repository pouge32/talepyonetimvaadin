package com.example.base.ui.LoginScreen;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.base.entity.Role;
import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("kayit-ol")
@AnonymousAllowed  
public class KayitOlView extends VerticalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; 
    private final SystemLogService systemLogService;

    public KayitOlView(UserRepository userRepository, PasswordEncoder passwordEncoder, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H2 title = new H2("Yeni Müşteri Kaydı");

        TextField nameField = new TextField("Ad Soyad");
        nameField.setWidthFull();

        EmailField emailField = new EmailField("E-Posta Adresi");
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField("Şifre");
        passwordField.setWidthFull();

        Button submitButton = new Button("Kayıt Oluştur");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();
        
        RouterLink loginLink = new RouterLink("Zaten üye misiniz? Giriş yapın", LoginView.class); 

        submitButton.addClickListener(e -> {
            if (nameField.isEmpty() || emailField.isEmpty() || passwordField.isEmpty()) {
                Notification.show("Lütfen tüm alanları doldurun!", 3000, Notification.Position.TOP_CENTER);
                return;
            }

            if (userRepository.findByEmail(emailField.getValue()).isPresent()) {
                Notification error = Notification.show("Bu e-posta adresi zaten kayıtlı!", 3000, Notification.Position.TOP_CENTER);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            String sifrelenmisSifre = passwordEncoder.encode(passwordField.getValue());

            UserEntity newUser = new UserEntity();
            newUser.setNameSurname(nameField.getValue());
            newUser.setEmail(emailField.getValue());
            newUser.setPassword(sifrelenmisSifre); 
            newUser.setPasswordHash(sifrelenmisSifre); 
            newUser.setRole(Role.CUSTOMER);
            newUser.setStatus("ONAY_BEKLIYOR"); 

            userRepository.save(newUser);

            systemLogService.log("Yeni müşteri kayıt başvurusu oluşturuldu: " + emailField.getValue());

            Notification success = Notification.show("Kayıt başarılı! Destek ekibi onayladıktan sonra giriş yapabilirsiniz.", 5000, Notification.Position.TOP_CENTER);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            nameField.clear();
            emailField.clear();
            passwordField.clear();
        });

        VerticalLayout formContainer = new VerticalLayout(nameField, emailField, passwordField, submitButton);
        formContainer.setPadding(false);
        formContainer.setMaxWidth("400px");
        formContainer.setWidthFull();
        formContainer.setAlignItems(FlexComponent.Alignment.STRETCH);

        add(title, formContainer, loginLink);
    }
}