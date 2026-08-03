package com.example.base.ui.CustomerScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Class.TalepChat;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "taleplerim", layout = MainLayout.class)
@RolesAllowed(value = "CUSTOMER")
public class TaleplerimView extends VerticalLayout {

    private final RequestService requestService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final RequestRepository requestRepository;
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);

    public TaleplerimView(RequestService requestService, ChatService chatService, 
                          UserRepository userRepository, SystemLogService systemLogService,
                          RequestRepository requestRepository) {
        this.requestService = requestService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.requestRepository = requestRepository;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        systemLogService.log("Müşteri (" + email + ") taleplerim sayfasını görüntüledi.");

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                  .set("overflow", "hidden");

        configureGrid();

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "24px")
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-sizing", "border-box");

        grid.setWidthFull();
        grid.getStyle()
                .set("flex-grow", "1")
                .set("background-color", "#ffffff")
                .set("border-radius", "12px");

        mainContainer.add(buildPageHeader(), grid);
        add(mainContainer);
    }

    private Div buildPageHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("margin-bottom", "16px").set("flex-shrink", "0");

        H2 heading = new H2("Taleplerim");
        heading.getStyle().set("margin", "0 0 2px 0").set("color", "var(--lumo-header-text-color)").set("font-size", "22px");

        Paragraph subtitle = new Paragraph("Geçmişte oluşturduğunuz tüm talepleri listeleyebilir, durumlarını ve sohbet geçmişlerini takip edebilirsiniz.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        header.add(heading, subtitle);
        return header;
    }

    private void configureGrid() {
        grid.addColumn(RequestEntity::getRequestId).setHeader("ID").setAutoWidth(true);
        Grid.Column<RequestEntity> titleColumn = grid.addColumn(RequestEntity::getTitle).setHeader("Başlık");
        Grid.Column<RequestEntity> descColumn = grid.addColumn(RequestEntity::getDescription).setHeader("Detay");
        
        grid.addComponentColumn(this::createScreenshotButton)
                .setHeader("Ekran Görüntüsü").setAutoWidth(true).setFlexGrow(0);

        Grid.Column<RequestEntity> dateColumn = grid.addColumn(RequestEntity::getCreatedAt).setHeader("Oluşturulma Tarihi");
        Grid.Column<RequestEntity> statusColumn = grid.addComponentColumn(this::createStatusBadge).setHeader("Durum").setAutoWidth(true);

        grid.addComponentColumn(this::createChatButton).setHeader("Sohbet").setAutoWidth(true).setFlexGrow(0);

        // MÜŞTERİ MEMNUNİYETİ KOLONU
        grid.addComponentColumn(this::createRatingColumn).setHeader("Değerlendirme").setAutoWidth(true).setFlexGrow(0);

        GridListDataView<RequestEntity> dataView = grid.setItems(requestService.getMyRequestsForCurrentUser());
        RequestFilter requestFilter = new RequestFilter(dataView);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        requestFilter.setStartDate(oneWeekAgo);
        requestFilter.setEndDate(now);

        HeaderRow headerRow = grid.appendHeaderRow();

        headerRow.getCell(titleColumn).setComponent(
                createFilterHeader("Başlığa göre ara...", requestFilter::setTitle));
        
        headerRow.getCell(descColumn).setComponent(
                createFilterHeader("Detaya göre ara...", requestFilter::setDescription));
        
        headerRow.getCell(dateColumn).setComponent(
                createDateRangeFilterHeader(requestFilter));
        
        headerRow.getCell(statusColumn).setComponent(
                createStatusFilterHeader(requestFilter::setStatus));

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
        if ("KAPATILDI".equals(request.getStatus())) {
            if (request.getSatisfactionScore() != null) {
                Span pointBadge = new Span("⭐ " + request.getSatisfactionScore() + "/5");
                pointBadge.getElement().getThemeList().add("badge success");
                pointBadge.getStyle().set("font-weight", "bold");
                return pointBadge;
            } else {
                Button rateBtn = new Button("Puan Ver", VaadinIcon.STAR.create());
                rateBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                rateBtn.addClickListener(e -> openRatingDialog(request));
                return rateBtn;
            }
        }
        return new Span("-");
    }

    private void openRatingDialog(RequestEntity request) {
        Dialog ratingDialog = new Dialog();
        ratingDialog.setHeaderTitle("Talebiniz Kapatıldı. Bizi Değerlendirin!");
        ratingDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();

        RadioButtonGroup<Integer> scoreGroup = new RadioButtonGroup<>();
        scoreGroup.setLabel("Hizmetimizden ne kadar memnun kaldınız?");
        scoreGroup.setItems(1, 2, 3, 4, 5);
        scoreGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        
        TextArea commentArea = new TextArea("Yorumunuz (İsteğe Bağlı)");
        commentArea.setPlaceholder("Bize nasıl daha iyi olabileceğimizi söyleyin...");
        commentArea.setWidthFull();

        layout.add(scoreGroup, commentArea);
        ratingDialog.add(layout);

        Button submitBtn = new Button("Değerlendirmeyi Gönder", event -> {
            if (scoreGroup.getValue() == null) {
                Notification.show("Lütfen 1 ile 5 arasında bir puan seçin.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            request.setSatisfactionScore(scoreGroup.getValue());
            request.setSatisfactionComment(commentArea.getValue());
            requestRepository.save(request);

            systemLogService.log("Talep ID: " + request.getRequestId() + " müşteri tarafından " + scoreGroup.getValue() + " yıldız ile değerlendirildi.");

            Notification.show("Değerlendirmeniz için teşekkür ederiz!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            ratingDialog.close();
            // Filtreleri bozmadan sadece ilgili satırı günceller
            grid.getDataProvider().refreshItem(request); 
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Kapat", e -> ratingDialog.close());

        ratingDialog.getFooter().add(submitBtn, cancelBtn);
        ratingDialog.open();
    }

    // --- MÜŞTERİ MEMNUNİYETİ METOTLARI BİTİŞ ---

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

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button("Sohbet", VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        com.vaadin.flow.component.html.Div container = new com.vaadin.flow.component.html.Div(chatButton);
        container.getStyle().set("position", "relative").set("display", "inline-block");

        org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : "";
        
        userRepository.findByEmail(email).ifPresent(currentUser -> {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.getStyle()
                        .set("position", "absolute")
                        .set("top", "-5px")
                        .set("right", "-5px")
                        .set("padding", "2px 6px")
                        .set("font-size", "10px")
                        .set("font-weight", "bold");
                container.add(badge);
            }
        });

        chatButton.addClickListener(e -> {
            systemLogService.log("Müşteri (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine giriş yaptı.");
            e.getSource().getUI().ifPresent(ui ->
                    ui.navigate(TalepChat.class, request.getRequestId()));
        });

        return container;
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

    private static Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false);
        dateLayout.setSpacing(true);

        DateTimePicker startPicker = new DateTimePicker("Başlangıç");
        startPicker.setWidthFull();
        startPicker.setValue(LocalDateTime.now().minusWeeks(1));
        startPicker.getElement().executeJs("this.inputElement.setAttribute('readonly', true);");
        startPicker.addValueChangeListener(e -> requestFilter.setStartDate(e.getValue()));

        DateTimePicker endPicker = new DateTimePicker("Bitiş");
        endPicker.setWidthFull();
        endPicker.setValue(LocalDateTime.now());
        endPicker.getElement().executeJs("this.inputElement.setAttribute('readonly', true);");
        endPicker.addValueChangeListener(e -> requestFilter.setEndDate(e.getValue()));

        dateLayout.add(startPicker, endPicker);
        return dateLayout;
    }

    private static Component createStatusFilterHeader(Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems("Yeni", "İncelemede", "İşleme Alındı", "KAPATILDI");
        comboBox.setPlaceholder("Durum seç...");
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    private static class RequestFilter {
        private final GridListDataView<RequestEntity> dataView;

        private String title = "";
        private String description = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String status = "";

        public RequestFilter(GridListDataView<RequestEntity> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setTitle(String title) {
            this.title = title != null ? title : "";
            this.dataView.refreshAll();
        }

        public void setDescription(String description) {
            this.description = description != null ? description : "";
            this.dataView.refreshAll();
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
            this.dataView.refreshAll();
        }

        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
            this.dataView.refreshAll();
        }

        public void setStatus(String status) {
            this.status = status != null ? status : "";
            this.dataView.refreshAll();
        }

        public boolean test(RequestEntity request) {
            boolean matchesTitle = matches(request.getTitle(), title);
            boolean matchesDesc = matches(request.getDescription(), description);
            
            boolean matchesDate = true;
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) {
                    matchesDate = false;
                }
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) {
                    matchesDate = false;
                }
            }

            String mappedStatus = switch (request.getStatus() != null ? request.getStatus() : "") {
                case "NEW" -> "Yeni";
                case "İncelemede" -> "İncelemede";
                case "İş Akışına Dönüştü" -> "İşleme Alındı";
                case "KAPATILDI" -> "KAPATILDI";
                default -> request.getStatus();
            };
            boolean matchesStatus = status.isEmpty() || mappedStatus.equalsIgnoreCase(status);

            return matchesTitle && matchesDesc && matchesDate && matchesStatus;
        }

        private boolean matches(String value, String searchTerm) {
            if (searchTerm == null || searchTerm.isEmpty()) {
                return true;
            }
            if (value == null) {
                return false;
            }
            return value.toLowerCase().contains(searchTerm.toLowerCase());
        }
    }

    private Badge createStatusBadge(RequestEntity request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        Badge badge = new Badge(statusLabel(status));
        String tooltipText = "";

        switch (status) {
            case "NEW":
                badge.addThemeVariants(BadgeVariant.CONTRAST);
                tooltipText = "Ön İnceleme Bekliyor. Tahmini Değerlendirme: 1-2 İş Günü";
                break;
            case "INCELEMEDE":
            case "İncelemede":
                badge.addThemeVariants(BadgeVariant.WARNING);
                tooltipText = "Ürün Yönetimi (PO) İncelemesinde. Tahmini Çözüm Süresi: 2-4 İş Günü";
                break;
            case "ONAYLANDI":
            case "İş Akışına Dönüştü":
                badge.addThemeVariants(BadgeVariant.SUCCESS);
                tooltipText = "Yazılım Ekibine Aktarıldı. Mevcut sprint eforuna göre kodlanacak.";
                break;
            case "KAPATILDI":
                badge.addThemeVariants(BadgeVariant.ERROR);
                tooltipText = "Bu talep sonuçlandırılarak kapatılmıştır.";
                break;
            default:
                badge.addThemeVariants(BadgeVariant.ERROR);
                tooltipText = "Durum değerlendiriliyor.";
                break;
        }

        badge.getElement().setProperty("title", tooltipText);
        
        badge.getStyle().set("cursor", "help");

        return badge;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "NEW" -> "Yeni";
            case "İncelemede" -> "İncelemede";
            case "İş Akışına Dönüştü" -> "İşleme Alındı";
            case "KAPATILDI" -> "KAPATILDI";
            default -> status;
        };
    }
}