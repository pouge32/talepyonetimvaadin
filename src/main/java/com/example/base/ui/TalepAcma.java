package com.example.base.ui;

import com.example.base.repository.UserRepository;
import com.example.base.service.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-olustur", layout = MainLayout.class)
@RolesAllowed("CUSTOMER")
public class TalepAcma extends VerticalLayout {

    private TextField title = new TextField("Talep Başlığı");
    private TextArea description = new TextArea("Talep Detayı");
    private Button submitButton = new Button("Gönder");

    private final RequestService requestService;
    private final UserRepository userRepository;

    private Binder<RequestFormDto> binder = new Binder<>(RequestFormDto.class);
    private RequestFormDto currentDto = new RequestFormDto();

    public TalepAcma(RequestService requestService, UserRepository userRepository) {
        this.requestService = requestService;
        this.userRepository = userRepository;

        FormLayout formLayout = new FormLayout();
        formLayout.add(title, description);

        setupBinder();

        submitButton.addClickListener(event -> saveRequest());

        add(formLayout, submitButton);
    }

    private void setupBinder() {
        binder.forField(title)
                .asRequired("Başlık boş olamaz")
                .withValidator(t -> t.trim().length() >= 5, "Başlık en az 5 karakter olmalıdır")
                .bind(RequestFormDto::getTitle, RequestFormDto::setTitle);

        binder.forField(description)
                .asRequired("Detay boş olamaz")
                .withValidator(d -> d.trim().length() >= 10, "Açıklama en az 10 karakter olmalıdır")
                .bind(RequestFormDto::getDescription, RequestFormDto::setDescription);

        binder.setBean(currentDto);
    }

    private void saveRequest() {
        if (binder.isValid()) {
            try {
                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                String loggedInEmail = (auth != null) ? auth.getName() : "";

                com.example.base.entity.UserEntity customerUser = userRepository.findByEmail(loggedInEmail)
                        .orElseThrow(() -> new RuntimeException("Giriş yapan kullanıcı bulunamadı!"));

                requestService.createRequest(
                        customerUser.getUserId(),
                        currentDto.getTitle(),
                        currentDto.getDescription()
                );

                Notification.show("Talebiniz başarıyla oluşturuldu!", 3000, Notification.Position.TOP_CENTER);

                currentDto = new RequestFormDto();
                binder.setBean(currentDto);

            } catch (Exception e) {
                Notification.show("Hata oluştu: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
            }
        } else {
            binder.validate();
            Notification.show("Lütfen formdaki hataları düzeltin.", 3000, Notification.Position.MIDDLE);
        }
    }

    public static class RequestFormDto {
        private String title;
        private String description;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}