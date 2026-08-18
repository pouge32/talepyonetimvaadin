package com.example.base.ui.MainScreen;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("bakim")
@AnonymousAllowed
@CssImport("./styles/main/bakim.css")
public class BakimView extends VerticalLayout implements HasDynamicTitle {
    
    public BakimView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setSpacing(true);
        addClassName("bakim-layout");
        
        H1 title = new H1("🛠️ Sistem Bakımda");
        title.addClassName("bakim-title");
        
        Paragraph text = new Paragraph("Sistemimizde şu anda planlı bir bakım çalışması yapılmaktadır. Lütfen daha sonra tekrar deneyiniz.");
        text.addClassName("bakim-text");

        Button homeButton = new Button("Giriş Ekranına Dön", VaadinIcon.HOME.create(), e -> {
            UI.getCurrent().navigate("login");
        });
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        homeButton.addClassName("bakim-home-btn");

        add(title, text, homeButton);
    }
    
    @Override
    public String getPageTitle() {
        return "Sistem Bakımda";
    }
}