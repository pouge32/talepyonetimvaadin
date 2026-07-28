package com.example.base.ui;

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

    public AccessDeniedView() {
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
        errorMessage.setText("Erişimi reddedilen adres: /" + event.getLocation().getPath());
        errorMessage.getStyle().set("font-size", "0.9em").set("color", "gray");
        return 403;
    }
}