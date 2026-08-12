package com.example.base.ui.MainScreen;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
public class BakimView extends VerticalLayout implements HasDynamicTitle {
    
    public BakimView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle().set("background-color", "var(--lumo-base-color)");
        setSpacing(true);
        
        H1 title = new H1("🛠️ Sistem Bakımda");
        title.getStyle().set("color", "var(--lumo-error-color)").set("margin-bottom", "0");
        
        Paragraph text = new Paragraph("Sistemimizde şu anda planlı bir bakım çalışması yapılmaktadır. Lütfen daha sonra tekrar deneyiniz.");
        text.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        Button homeButton = new Button("Giriş Ekranına Dön", VaadinIcon.HOME.create(), e -> {
            UI.getCurrent().navigate("login");
        });
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        homeButton.getStyle().set("margin-top", "20px");

        add(title, text, homeButton);
    }
    
    @Override
    public String getPageTitle() {
        return "Sistem Bakımda";
    }
}