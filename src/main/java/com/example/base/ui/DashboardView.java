package com.example.base.ui;

import com.example.base.entity.RequestEntity;
import com.example.base.service.DashboardService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "dashboard", layout = MainLayout.class)
@RolesAllowed("ADMIN")

public class DashboardView extends VerticalLayout {

    private final DashboardService dashboardService;

    private final Grid<RequestEntity> topRequestsGrid = new Grid<>(RequestEntity.class, false); 

    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;

        add(new H1("Yönetici Paneli"));

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        cardsLayout.add(
            createCard("Bekleyen Talep Sayısı",
                dashboardService.getBekleyenTalepSayisi(),
                "var(--lumo-primary-color)"),

            createCard("İş Akışına Dönüşen Görev Sayısı",
                dashboardService.getIsAkisinaDonusenTalepSayisi(),
                "var(--lumo-success-color)"),

            createCard("Acil Müdahale Bekleyenler",
                dashboardService.getAcilMudahaleBekleyenSayisi(),
                "var(--lumo-error-color)")
        );

        add(cardsLayout);

        add(new H3("En Yüksek Öncelikli 5 Talep"));
        configureTopRequestsGrid();
        add(topRequestsGrid);

        refreshTopRequestsGrid();
    }

    private VerticalLayout createCard(String baslik, long deger, String renk) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
            LumoUtility.Background.BASE,
            LumoUtility.BorderRadius.LARGE,
            LumoUtility.BoxShadow.SMALL,
            LumoUtility.Padding.LARGE
        );
        card.setWidth("280px");
        card.setSpacing(false);
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");

        Span titleSpan = new Span(baslik);
        titleSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        H2 valueLabel = new H2(String.valueOf(deger));
        valueLabel.getStyle().set("margin", "0.2em 0 0 0");
        valueLabel.getStyle().set("font-weight", "700");
        valueLabel.getStyle().set("color", renk);

        card.add(titleSpan, valueLabel);
        return card;
    }

    private void configureTopRequestsGrid() {
        topRequestsGrid.addColumn(RequestEntity::getRequestId).setHeader("ID").setAutoWidth(true);
        topRequestsGrid.addColumn(RequestEntity::getTitle).setHeader("Başlık");
        topRequestsGrid.addColumn(RequestEntity::getDescription).setHeader("Detay");
        topRequestsGrid.addColumn(RequestEntity::getStatus).setHeader("Durum").setAutoWidth(true);
        topRequestsGrid.addColumn(RequestEntity::getCreatedAt).setHeader("Oluşturulma Tarihi");

        topRequestsGrid.setHeight("300px");
        topRequestsGrid.setWidthFull();
    }

    private void refreshTopRequestsGrid() {
        topRequestsGrid.setItems(dashboardService.getEnYuksekOncelikliIlk5Talep());
    }
    
}