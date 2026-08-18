package com.example.base.ui.MainScreen.ProfilScreen;

import com.example.base.entity.UserEntity;
import com.example.base.service.UserService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class ProfilKvkkHelper {

    private final UserService userService;
    private final Component context;

    public ProfilKvkkHelper(UserService userService, Component context) {
        this.userService = userService;
        this.context = context;
    }

    public Button buildKvkkDeleteButton(UserEntity user) {
        Button deleteDataBtn = new Button(context.getTranslation("profile.kvkk.deleteBtn"), VaadinIcon.TRASH.create());
        deleteDataBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        if (Boolean.TRUE.equals(user.getDeletionRequested())) {
            deleteDataBtn.setEnabled(false);
            deleteDataBtn.setText(context.getTranslation("profile.kvkk.pendingBtn")); 
        }

        deleteDataBtn.addClickListener(e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader(context.getTranslation("profile.kvkk.dialog.header"));
            dialog.setText(context.getTranslation("profile.kvkk.dialog.text"));
            
            dialog.setCancelable(true);
            dialog.setCancelText(context.getTranslation("profile.kvkk.dialog.cancel"));
            
            dialog.setConfirmText(context.getTranslation("profile.kvkk.dialog.confirm"));
            dialog.setConfirmButtonTheme("error primary");

            dialog.addConfirmListener(confirmEvent -> {
                try {
                    userService.requestAccountDeletion(user.getUserId());
                    
                    Notification.show(context.getTranslation("profile.kvkk.notif.success"), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    deleteDataBtn.setEnabled(false);
                    deleteDataBtn.setText(context.getTranslation("profile.kvkk.pendingBtn"));
                    
                } catch (Exception ex) {
                    Notification.show("Hata: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            dialog.open();
        });

        return deleteDataBtn;
    }
}