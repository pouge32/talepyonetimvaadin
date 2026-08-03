package com.example.base.ui.MainScreen;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.service.SystemLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

import jakarta.annotation.security.PermitAll;

@PermitAll
public class AccessDeniedView extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {

    private final Span errorMessage = new Span();
    private final SystemLogService systemLogService;

    public AccessDeniedView(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        H1 baslik = new H1("403 - Yetkisiz Erişim");
        baslik.getStyle().set("color", "var(--lumo-error-text-color)");

        Span aciklama = new Span("Bu sayfayı görüntülemek için gerekli yetkilere sahip değilsiniz.");
        
        Button anasayfaButonu = new Button("Ana Sayfaya Dön", VaadinIcon.HOME.create());
        anasayfaButonu.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));
        anasayfaButonu.getStyle().set("margin-top", "20px");

        add(baslik, aciklama, errorMessage, anasayfaButonu);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        String targetPath = "/" + event.getLocation().getPath();
        errorMessage.setText("Erişimi reddedilen adres: " + targetPath);
        errorMessage.getStyle().set("font-size", "0.9em").set("color", "gray");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (auth != null && auth.isAuthenticated()) ? auth.getName() : "Anonim";
        systemLogService.log("Güvenlik Uyarısı: " + userEmail + " kullanıcısı yetkisiz olarak '" + targetPath + "' sayfasına erişmeye çalıştı (403).");

        return 403;
    }
}