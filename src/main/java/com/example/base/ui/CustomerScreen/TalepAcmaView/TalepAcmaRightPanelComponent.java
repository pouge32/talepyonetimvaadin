package com.example.base.ui.CustomerScreen.TalepAcmaView;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TalepAcmaRightPanelComponent extends VerticalLayout {

    public TalepAcmaRightPanelComponent() {
        setPadding(false);
        setSpacing(true);
        add(buildTipsCard(), buildCategoryLegendCard());
    }

    private Div createCard() {
        Div card = new Div();
        card.setWidthFull();
        card.addClassName("talep-acma-card");
        return card;
    }

    private Div iconBadge(VaadinIcon vaadinIcon, String colorVar) {
        Div badge = new Div();
        Icon icon = vaadinIcon.create();
        icon.setSize("18px");
        icon.getStyle().set("color", colorVar);
        badge.addClassName("talep-acma-icon-badge");
        badge.getStyle().set("background-color", "color-mix(in srgb, " + colorVar + " 12%, white)");
        badge.add(icon);
        return badge;
    }

    private Div buildTipsCard() {
        Div card = createCard();

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        Div badge = iconBadge(VaadinIcon.LIGHTBULB, "var(--lumo-success-color)");
        H3 title = new H3(getTranslation("request.create.tips.title"));
        title.addClassName("talep-acma-card-title");
        headerRow.add(badge, title);

        VerticalLayout tips = new VerticalLayout();
        tips.setPadding(false);
        tips.setSpacing(true);
        tips.addClassName("talep-acma-tips-layout");

        String[] tipTexts = {
                getTranslation("request.create.tips.1"),
                getTranslation("request.create.tips.2"),
                getTranslation("request.create.tips.3")
        };

        for (String t : tipTexts) {
            HorizontalLayout row = new HorizontalLayout();
            row.setSpacing(true);
            row.setAlignItems(FlexComponent.Alignment.START);
            
            Icon dot = VaadinIcon.CHECK_CIRCLE.create();
            dot.setSize("14px");
            dot.addClassName("talep-acma-tip-dot");
            
            Span text = new Span(t);
            text.addClassName("talep-acma-tip-text");
            
            row.add(dot, text);
            tips.add(row);
        }

        card.add(headerRow, tips);
        return card;
    }

    private Div buildCategoryLegendCard() {
        Div card = createCard();

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        Div badge = iconBadge(VaadinIcon.TAGS, "var(--lumo-warning-color)");
        H3 title = new H3(getTranslation("request.create.legend.title"));
        title.addClassName("talep-acma-card-title");
        headerRow.add(badge, title);

        VerticalLayout legend = new VerticalLayout();
        legend.setPadding(false);
        legend.setSpacing(false);
        legend.addClassName("talep-acma-legend-layout");

        for (TalepAcma.Kategori k : TalepAcma.Kategori.values()) {
            Div row = new Div();
            row.addClassName("talep-acma-legend-row");
            
            Span name = new Span(k.getLabel());
            name.addClassName("talep-acma-legend-name");
            
            Span hint = new Span(k.getHint());
            hint.addClassName("talep-acma-legend-hint");
            
            row.add(name, hint);
            legend.add(row);
        }

        card.add(headerRow, legend);
        return card;
    }
}