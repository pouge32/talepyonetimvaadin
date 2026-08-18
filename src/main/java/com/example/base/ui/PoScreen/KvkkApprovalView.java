package com.example.base.ui.PoScreen;

import java.time.format.DateTimeFormatter;

import com.example.base.entity.UserEntity;
import com.example.base.service.UserService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "kvkk-talepleri", layout = MainLayout.class)
@RolesAllowed({"PO", "ADMIN", "GODPANEL"})
@PageTitle("KVKK Silme Talepleri | Monad")
@CssImport("./styles/po/kvkk-approval.css")
public class KvkkApprovalView extends VerticalLayout {

    private final UserService userService;
    private final Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

    public KvkkApprovalView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("KVKK Veri Silme Talepleri");
        
        Span subtitle = new Span("Hesabının anonimleştirilmesini talep eden müşteriler.");
        subtitle.addClassName("kvkk-subtitle");

        configureGrid();
        refreshGrid();

        add(header, subtitle, grid);
    }

    private void configureGrid() {
        grid.addColumn(UserEntity::getUserId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(UserEntity::getNameSurname).setHeader("Müşteri Adı");
        grid.addColumn(UserEntity::getEmail).setHeader("E-Posta");
        
        grid.addColumn(user -> user.getDeletionRequestDate() != null 
                ? user.getDeletionRequestDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) 
                : "").setHeader("Talep Tarihi");

        grid.addComponentColumn(this::createActionButtons).setHeader("İşlemler").setAutoWidth(true);
    }

    private HorizontalLayout createActionButtons(UserEntity user) {
        Button approveBtn = new Button("Anonimleştir", VaadinIcon.TRASH.create());
        approveBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        approveBtn.addClickListener(e -> confirmAnonymization(user));

        Button rejectBtn = new Button("Reddet", VaadinIcon.CLOSE.create());
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        rejectBtn.addClickListener(e -> {
            userService.rejectKvkkDeletion(user.getUserId());
            Notification.show("Talep reddedildi.", 3000, Notification.Position.TOP_CENTER);
            refreshGrid();
        });

        return new HorizontalLayout(approveBtn, rejectBtn);
    }

    private void confirmAnonymization(UserEntity user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Kullanıcıyı Anonimleştir");
        dialog.setText(user.getNameSurname() + " isimli kullanıcının tüm kişisel verileri maskelenecektir. Bu işlem geri döndürülemez. Onaylıyor musunuz?");
        
        dialog.setCancelable(true);
        dialog.setCancelText("İptal");
        
        dialog.setConfirmText("Evet, Anonimleştir");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(e -> {
            userService.approveKvkkDeletion(user.getUserId());
            Notification.show("Kullanıcı verileri KVKK kapsamında anonimleştirildi.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        });
        
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(userService.getPendingDeletionRequests());
    }
}