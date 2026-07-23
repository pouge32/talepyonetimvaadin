package com.example.base.ui;

import com.example.base.service.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

@Route("talep-olustur")
public class TalepAcma extends VerticalLayout {

    private final RequestService requestService;
    
    private TextField title = new TextField("Talep Başlığı");
    private TextArea description = new TextArea("Talep Detayı");
    private Button submitButton = new Button("Gönder");

    private Binder<RequestFormDto> binder = new Binder<>(RequestFormDto.class);
    private RequestFormDto currentDto = new RequestFormDto();

    public TalepAcma(RequestService requestService) {
        this.requestService = requestService;

        FormLayout formLayout = new FormLayout();
        formLayout.add(title, description);
        
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        setupBinder();

        submitButton.addClickListener(event -> saveRequest());

        add(formLayout, submitButton);
    }

    private void setupBinder() {
        binder.bindInstanceFields(this);
        binder.setBean(currentDto);
    }

    private void saveRequest() {
        if (binder.isValid()) {
            try {
                Integer currentCustomerId = 1; 

                requestService.createRequest(
                        currentCustomerId, 
                        currentDto.getTitle(), 
                        currentDto.getDescription()
                );

                Notification success = Notification.show("Talebiniz başarıyla oluşturuldu!", 3000, Notification.Position.TOP_CENTER);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                currentDto = new RequestFormDto();
                binder.setBean(currentDto);

            } catch (Exception e) {
                Notification error = Notification.show("Hata oluştu: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            Notification.show("Lütfen gerekli alanları doldurun.", 3000, Notification.Position.MIDDLE);
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