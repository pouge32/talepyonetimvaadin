package com.example.base.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class) 
@PermitAll 
public class HomeView extends VerticalLayout {

    public HomeView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        H2 baslik = new H2("Talep Yönetim Sistemine Hoş Geldiniz!");
        Span aciklama = new Span("Lütfen işleminize devam etmek için sol taraftaki menüyü kullanın.");
        
        add(baslik, aciklama);
    }
}