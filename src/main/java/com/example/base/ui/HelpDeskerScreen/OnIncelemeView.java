package com.example.base.ui.HelpDeskerScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatService;
import com.example.base.service.NotificationService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Class.TalepChat;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import java.time.temporal.ChronoUnit;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "on-inceleme", layout = MainLayout.class)
@RolesAllowed("HELPDESK")
public class OnIncelemeView extends VerticalLayout {

    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);

    private GridListDataView<RequestEntity> dataView;
    private final RequestFilter requestFilter = new RequestFilter();
    
    private RequestEntity selectedRequest;
    private Dialog closeDialog = new Dialog();
    private TextArea closeReason = new TextArea("Kapatma / Red Nedeni");

    public OnIncelemeView(RequestRepository requestRepository, NotificationService notificationService, 
                          ChatService chatService, UserRepository userRepository, SystemLogService systemLogService) {
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;

        add(new H3("Ön İnceleme ve Triyaj (Destek Ekranı)"));

        configureGrid();
        configureCloseDialog();

        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle).setHeader("Başlık");
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription).setHeader("Detay");
        
        // DURUM KOLONU EKLENDİ
        Grid.Column<RequestEntity> statusCol = grid.addComponentColumn(this::createStatusBadge).setHeader("Durum").setAutoWidth(true);
        Grid.Column<RequestEntity> dateCol = grid.addColumn(RequestEntity::getCreatedAt).setHeader("Oluşturulma Tarihi");
        

        grid.addComponentColumn(this::createScreenshotButton)
                .setHeader("Ekran Görüntüsü").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            Button chatButton = new Button("Sohbet", VaadinIcon.CHAT.create());
            chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Div container = new Div(chatButton);
            container.getStyle().set("position", "relative").set("display", "inline-block");

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = (auth != null) ? auth.getName() : "";
            UserEntity currentUser = userRepository.findByEmail(email).orElse(null);

            if (currentUser != null) {
                int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
                if (unreadCount > 0) {
                    Span badge = new Span(String.valueOf(unreadCount));
                    badge.getElement().getThemeList().add("badge error primary pill");
                    badge.getStyle().set("position", "absolute").set("top", "-5px").set("right", "-5px")
                            .set("padding", "2px 6px").set("font-size", "10px").set("font-weight", "bold");
                    container.add(badge);
                }
            }

            chatButton.addClickListener(e -> {
                systemLogService.log("Destek Personeli (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine girdi.");
                e.getSource().getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId()));
            });
            return container;
        }).setHeader("Sohbet").setAutoWidth(true);

        // DEĞERLENDİRME (MEMNUNİYET) KOLONU (SADECE OKUMA)
        grid.addComponentColumn(this::createRatingColumn).setHeader("Değerlendirme").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(request -> {
            // SADECE "NEW" (Yeni) STATÜSÜNDEKİ TALEPLERDE İŞLEM YAPILABİLİR
            if (!"NEW".equals(request.getStatus())) {
                return new Span("-"); 
            }

            Button closeBtn = new Button("Kapat", VaadinIcon.CLOSE_CIRCLE.create());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            closeBtn.addClickListener(e -> {
                selectedRequest = request;
                closeDialog.open();
            });

            Button sendToPoBtn = new Button("PO'ya Sevk Et", VaadinIcon.ARROW_RIGHT.create());
            sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            sendToPoBtn.addClickListener(e -> forwardToPo(request));

            return new HorizontalLayout(closeBtn, sendToPoBtn);
        }).setHeader("İşlemler").setAutoWidth(true);

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(titleCol).setComponent(createFilterHeader("Başlığa göre ara...", requestFilter::setTitle));
        headerRow.getCell(descCol).setComponent(createFilterHeader("Detaya göre ara...", requestFilter::setDescription));
        headerRow.getCell(statusCol).setComponent(createStatusFilterHeader(requestFilter::setStatus));
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));

        grid.addComponentColumn(this::createSlaBadge).setHeader("SLA Durumu").setAutoWidth(true).setFlexGrow(0);
    }

    private Badge createSlaBadge(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Badge closedBadge = new Badge("Tamamlandı");
            closedBadge.addThemeVariants(BadgeVariant.CONTRAST); 
            return closedBadge;
        }

        long hoursElapsed = ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());

        
        long slaLimitHours = 24; 
        long warningLimitHours = (long) (slaLimitHours * 0.75);

        if (hoursElapsed >= slaLimitHours) {
            Badge ihlalBadge = new Badge("İHLAL (" + hoursElapsed + "s)");
            ihlalBadge.addThemeVariants(BadgeVariant.ERROR);
            ihlalBadge.getElement().setProperty("title", "SLA Süresi Aşıldı!");
            return ihlalBadge;
        } else if (hoursElapsed >= warningLimitHours) {
            Badge uyariBadge = new Badge("YAKLAŞIYOR (" + hoursElapsed + "s)");
            uyariBadge.addThemeVariants(BadgeVariant.WARNING);
            uyariBadge.getElement().setProperty("title", "SLA İhlaline Az Kaldı!");
            return uyariBadge;
        } else {
            Badge normalBadge = new Badge("NORMAL (" + hoursElapsed + "s)");
            normalBadge.addThemeVariants(BadgeVariant.SUCCESS);
            return normalBadge;
        }
    }

    private Component createRatingColumn(RequestEntity request) {
        if (request.getSatisfactionScore() != null) {
            Span pointBadge = new Span("⭐ " + request.getSatisfactionScore() + "/5");
            pointBadge.getElement().getThemeList().add("badge success");
            pointBadge.getStyle().set("font-weight", "bold");
            
            if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                pointBadge.getElement().setProperty("title", "Yorum: " + request.getSatisfactionComment());
                pointBadge.getStyle().set("cursor", "help");
            }
            return pointBadge;
        } else if ("KAPATILDI".equals(request.getStatus())) {
            return new Span("Puanlanmamış");
        }
        return new Span("-");
    }

    // --- DURUM ROZETİ ---
    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(status);
        switch (status) {
            case "NEW": badge.addThemeVariants(BadgeVariant.CONTRAST); break;
            case "INCELEMEDE": badge.addThemeVariants(BadgeVariant.WARNING); break;
            case "ONAYLANDI": 
            case "İş Akışına Dönüştü": badge.addThemeVariants(BadgeVariant.SUCCESS); break;
            case "KAPATILDI": badge.addThemeVariants(BadgeVariant.ERROR); break;
            default: badge.addThemeVariants(BadgeVariant.CONTRAST); break;
        }
        return badge;
    }

    private Component createScreenshotButton(RequestEntity request) {
        boolean hasScreenshot = request.getScreenshotData() != null && request.getScreenshotData().length > 0;

        Button button = new Button(hasScreenshot ? "Görüntüle" : "Yok", VaadinIcon.PICTURE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        if (!hasScreenshot) {
            button.setEnabled(false);
            button.getElement().setProperty("title", "Bu talebe ekran görüntüsü eklenmemiş");
            return button;
        }

        button.getElement().setProperty("title", "Ekran görüntüsünü büyük görüntüle");
        button.addClickListener(e -> openScreenshotDialog(request));
        return button;
    }

    private void openScreenshotDialog(RequestEntity request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Ekran Görüntüsü - Talep #" + request.getRequestId());
        dialog.setWidth("640px");
        dialog.setCloseOnOutsideClick(true);

        String fileName = request.getScreenshotFileName() != null ? request.getScreenshotFileName() : "ekran-goruntusu.png";
        StreamResource resource = new StreamResource(fileName,
                () -> new ByteArrayInputStream(request.getScreenshotData()));

        Image image = new Image(resource, "Ekran görüntüsü");
        image.setWidthFull();
        image.getStyle()
                .set("max-height", "70vh")
                .set("object-fit", "contain")
                .set("border-radius", "8px");

        Button closeBtn = new Button("Kapat", e -> dialog.close());

        dialog.add(image);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private static Component createFilterHeader(String placeholder, Consumer<String> filterChangeConsumer) {
        TextField textField = new TextField();
        textField.setPlaceholder(placeholder);
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();
        textField.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return textField;
    }

    private static Component createStatusFilterHeader(Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        comboBox.setPlaceholder("Durum seç...");
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private static Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder("Başlangıç");
        startPicker.setClearButtonVisible(true);
        startPicker.setWidthFull();

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder("Bitiş");
        endPicker.setClearButtonVisible(true);
        endPicker.setWidthFull();

        startPicker.addValueChangeListener(e -> {
            endPicker.setMin(e.getValue());
            requestFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });

        endPicker.addValueChangeListener(e -> {
            startPicker.setMax(e.getValue());
            requestFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        HorizontalLayout layout = new HorizontalLayout(startPicker, endPicker);
        layout.setWidthFull();
        layout.setSpacing(false);
        return layout;
    }

    private void configureCloseDialog() {
        closeDialog.setHeaderTitle("Talebi Kapat / Reddet");
        closeReason.setWidthFull();

        Button confirmCloseBtn = new Button("Talebi Kapat", event -> {
            if (selectedRequest != null) {
                selectedRequest.setStatus("KAPATILDI");
                requestRepository.save(selectedRequest);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String staffEmail = (auth != null) ? auth.getName() : "";
                
                systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + selectedRequest.getRequestId() + " olan talebi kapattı. Gerekçe: " + closeReason.getValue());

                if (selectedRequest.getCustomer() != null) {
                    notificationService.notifyUser(selectedRequest.getCustomer().getUserId(), "Talebiniz Kapatıldı", "Açıklama: " + closeReason.getValue());
                }
                Notification.show("Talep kapatıldı.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                closeDialog.close();
                closeReason.clear();
                selectedRequest = null;
                refreshGrid();
            }
        });
        confirmCloseBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button("İptal", e -> closeDialog.close());

        closeDialog.getFooter().add(confirmCloseBtn, cancelBtn);
        closeDialog.add(closeReason);
    }

    private void forwardToPo(RequestEntity request) {
        request.setStatus("INCELEMEDE");
        requestRepository.save(request);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String staffEmail = (auth != null) ? auth.getName() : "";
        
        systemLogService.log("Destek Personeli (" + staffEmail + "), ID: " + request.getRequestId() + " olan talebi PO'ya sevk etti.");

        if (request.getCustomer() != null) {
            notificationService.notifyUser(request.getCustomer().getUserId(), "Talebiniz İnceleniyor", "Talebiniz ürün yönetimi (PO) havuzuna aktarıldı.");
        }
        Notification.show("Talep PO havuzuna sevk edildi!", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }

    private void refreshGrid() {
        // Tüm talepleri getiriyoruz (Sadece NEW olanları değil)
        dataView = grid.setItems(requestRepository.findAll());
        requestFilter.setDataView(dataView); 
    }

    private static class RequestFilter {
        private GridListDataView<RequestEntity> dataView;
        private String title = "";
        private String description = "";
        private String status = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public void setDataView(GridListDataView<RequestEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setTitle(String title) {
            this.title = title != null ? title : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setDescription(String description) {
            this.description = description != null ? description : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setStatus(String status) {
            this.status = status != null ? status : "";
            if (dataView != null) dataView.refreshAll();
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
            if (dataView != null) dataView.refreshAll();
        }

        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
            if (dataView != null) dataView.refreshAll();
        }

        public boolean test(RequestEntity request) {
            boolean matchesTitle = matches(request.getTitle(), title);
            boolean matchesDesc = matches(request.getDescription(), description);
            boolean matchesStatus = matches(request.getStatus(), status);
            boolean matchesDate = true;
            
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) matchesDate = false;
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) matchesDate = false;
            }
            return matchesTitle && matchesDesc && matchesStatus && matchesDate;
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty() || 
                   (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }
}