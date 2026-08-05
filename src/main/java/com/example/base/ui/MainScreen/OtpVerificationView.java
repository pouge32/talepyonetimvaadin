package com.example.base.ui.MainScreen;

import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.service.OtpService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import jakarta.annotation.security.PermitAll;

@Route("2fa-dogrulama")
@PermitAll
public class OtpVerificationView extends VerticalLayout implements HasDynamicTitle {

    public OtpVerificationView(OtpService otpService, UserRepository userRepository) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H2 title = new H2(getTranslation("otp.title"));
        Paragraph desc = new Paragraph(getTranslation("otp.description"));
        desc.getStyle().set("text-align", "center").set("color", "var(--lumo-secondary-text-color)");

        TextField otpField = new TextField(getTranslation("otp.fieldLabel"));
        otpField.setPlaceholder(getTranslation("otp.placeholder"));
        otpField.setMaxLength(6);

        Button verifyBtn = new Button(getTranslation("otp.verifyBtn"), e -> {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            UserEntity user = userRepository.findByEmail(email).orElse(null);

            if (user != null && otpService.validateOtp(user, otpField.getValue())) {
                VaadinSession.getCurrent().setAttribute("2FA_PASSED", true);
                Notification.show(getTranslation("otp.success"), 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                UI.getCurrent().navigate(""); 
            } else {
                Notification.show(getTranslation("otp.error"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button resendBtn = new Button(getTranslation("otp.resendBtn"), e -> {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            userRepository.findByEmail(email).ifPresent(otpService::generateAndSendOtp);
            Notification.show(getTranslation("otp.resendSuccess"), 2000, Notification.Position.TOP_CENTER);
        });
        resendBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(title, desc, otpField, verifyBtn, resendBtn);

        if (VaadinSession.getCurrent().getAttribute("OTP_SENT") == null) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            userRepository.findByEmail(email).ifPresent(otpService::generateAndSendOtp);
            VaadinSession.getCurrent().setAttribute("OTP_SENT", true);
        }
    }

    @Override
    public String getPageTitle() {
        return getTranslation("otp.pageTitle");
    }
}