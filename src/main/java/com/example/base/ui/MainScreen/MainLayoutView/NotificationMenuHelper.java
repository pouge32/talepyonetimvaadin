package com.example.base.ui.MainScreen.MainLayoutView;

import java.util.List;

import com.example.base.entity.NotificationEntity;
import com.example.base.service.NotificationService;
import com.example.base.ui.PoScreen.KvkkApprovalView;
import com.example.base.ui.PoScreen.TalepDegerlendirme.TalepDegerlendirmeView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class NotificationMenuHelper {

    private final MenuItem bellItem;
    private final NotificationService notificationService;

    public NotificationMenuHelper(MenuItem bellItem, NotificationService notificationService) {
        this.bellItem = bellItem;
        this.notificationService = notificationService;
    }

    public void updateNotificationsUI(Integer currentUserId) {
        if (currentUserId == null) return;

        bellItem.removeAll();
        bellItem.getSubMenu().removeAll();
        bellItem.add(VaadinIcon.BELL.create());

        List<NotificationEntity> notifs = notificationService.getUnreadNotifications(currentUserId);

        if (!notifs.isEmpty()) {
            Span badge = new Span(String.valueOf(notifs.size()));
            badge.getElement().getThemeList().add("badge error primary pill");
            badge.addClassName("main-layout-notif-badge");
            bellItem.add(badge);

            for (NotificationEntity notif : notifs) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidth("320px");
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                row.addClassName("main-layout-notif-row");

                Span text = new Span(notif.getTitle() + ": " + notif.getContent());
                text.addClassName("main-layout-notif-text");

                text.addClickListener(e -> {
                    String title = notif.getTitle();
                    if (title != null) {
                        if (title.contains("KVKK")) {
                            UI.getCurrent().navigate(KvkkApprovalView.class);
                        } else if (title.contains("Yeni Talep")) {
                            UI.getCurrent().navigate(TalepDegerlendirmeView.class);
                        }
                    }
                    notificationService.markAsRead(notif);
                    updateNotificationsUI(currentUserId);
                });

                Button checkBtn = new Button(VaadinIcon.CHECK.create());
                checkBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                checkBtn.getElement().setProperty("title", bellItem.getTranslation("notification.markAsRead"));
                checkBtn.addClickListener(e -> {
                    notificationService.markAsRead(notif);
                    row.addClassName("hidden-row");
                    row.setVisible(false);
                    updateNotificationsUI(currentUserId);
                });

                HorizontalLayout buttons = new HorizontalLayout(checkBtn);
                buttons.setSpacing(false);
                buttons.addClassName("main-layout-notif-buttons");

                row.add(text, buttons);

                MenuItem item = bellItem.getSubMenu().addItem(row);
                item.setEnabled(true);
            }
        } else {
            MenuItem emptyItem = bellItem.getSubMenu().addItem(bellItem.getTranslation("notification.empty"));
            emptyItem.setEnabled(false);
        }
    }
}