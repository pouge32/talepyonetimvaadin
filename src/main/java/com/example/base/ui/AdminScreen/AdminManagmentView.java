package com.example.base.ui.AdminScreen;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
import com.example.base.service.ChatService;
import com.example.base.service.ExcelExportService; // EKLENDİ
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.Class.TalepChat;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor; // EKLENDİ
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent; // EKLENDİ
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

@Route(value = "admin-paneli", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminManagmentView extends VerticalLayout {

    private final RequestService requestService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final SystemLogService systemLogService;
    private final ExcelExportService excelExportService; // EKLENDİ

    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    private GridListDataView<RequestEntity> dataView;
    private final RequestFilter requestFilter = new RequestFilter();

    // PO Önceliklendirme Araçları
    private Dialog poDialog = new Dialog("Detaylı Önceliklendirme (PO Yetkisi)");
    private ComboBox<String> urgency = new ComboBox<>("Aciliyet");
    private ComboBox<String> impact = new ComboBox<>("İş Etkisi");
    private ComboBox<String> effort = new ComboBox<>("Efor / Maliyet");
    private Checkbox securityOverride = new Checkbox("Kritik Güvenlik Kesintisi (Öncelik Puanı: 999)");
    private RequestEntity selectedRequestForPo;

    // Kapatma/Reddetme Araçları
    private Dialog closeDialog = new Dialog();
    private TextArea closeReason = new TextArea("Kapatma / Red Nedeni");
    private RequestEntity selectedRequestForClose;

    // Programmer / Durum Güncelleme Araçları
    private Dialog statusDialog = new Dialog("Durum Güncelle (Programmer/Sistem Yetkisi)");
    private ComboBox<String> statusCombo = new ComboBox<>("Yeni Durum");
    private RequestEntity selectedRequestForStatus;

    public AdminManagmentView(RequestService requestService, NotificationService notificationService,
                              ChatService chatService, UserRepository userRepository,
                              RequestRepository requestRepository, SystemLogService systemLogService,
                              ExcelExportService excelExportService) { // EKLENDİ
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.systemLogService = systemLogService;
        this.excelExportService = excelExportService; // EKLENDİ

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "24px")
                .set("max-width", "1600px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("display", "flex")
                .set("flex-direction", "column");

        grid.setWidthFull();
        grid.getStyle().set("flex-grow", "1").set("border-radius", "12px");

        configureGrid();
        configurePoDialog();
        configureCloseDialog();
        configureStatusDialog();

        mainContainer.add(buildHeader(), grid);
        add(mainContainer);

        refreshGrid();
    }

    private Div buildHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("flex-shrink", "0").set("margin-bottom", "16px");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        H2 title = new H2("Süper Yönetici (Admin) Paneli");
        title.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-header-text-color)");

        Paragraph subtitle = new Paragraph("Sistemdeki tüm taleplere tam erişim. Kapatma, önceliklendirme, durum değiştirme ve sohbet yetkileri.");
        subtitle.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

        textLayout.add(title, subtitle);

        // EXCEL EXPORT BUTONU (StreamResource ve Anchor ile güvenli indirme)
        Button exportBtn = new Button("Excel Raporu İndir", VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        StreamResource resource = new StreamResource(
            "Talep_Raporu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx", 
            () -> excelExportService.exportRequestsToExcel(requestRepository.findAll())
        );

        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(exportBtn);

        titleRow.add(textLayout, downloadLink);
        header.add(titleRow);

        return header;
    }
    
    private void configureGrid() {
        grid.addColumn(RequestEntity::getRequestId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        Grid.Column<RequestEntity> titleCol = grid.addColumn(RequestEntity::getTitle).setHeader("Başlık");
        Grid.Column<RequestEntity> descCol = grid.addColumn(RequestEntity::getDescription).setHeader("Detay");
        
        Grid.Column<RequestEntity> statusCol = grid.addComponentColumn(this::createStatusBadge).setHeader("Sistem Durumu").setAutoWidth(true);
        Grid.Column<RequestEntity> dateCol = grid.addColumn(RequestEntity::getCreatedAt).setHeader("Oluşturulma");

        // ADMİN EKRANI: DEĞERLENDİRME DÜZENLEME KOLONU
        grid.addComponentColumn(this::createRatingColumn).setHeader("Değerlendirme").setAutoWidth(true).setFlexGrow(0);

        // İşlem Kolonu (Tüm Yetkilerin Toplandığı Yer)
        grid.addComponentColumn(request -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button closeBtn = new Button(VaadinIcon.CLOSE_CIRCLE.create());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            closeBtn.getElement().setProperty("title", "Talebi Kapat / Reddet");
            closeBtn.addClickListener(e -> {
                selectedRequestForClose = request;
                closeDialog.open();
            });

            Button sendToPoBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
            sendToPoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            sendToPoBtn.getElement().setProperty("title", "PO İncelemesine Gönder (INCELEMEDE)");
            sendToPoBtn.addClickListener(e -> forwardToPo(request));

            Button poBtn = new Button(VaadinIcon.SLIDERS.create());
            poBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            poBtn.getElement().setProperty("title", "Önceliklendir & Onayla (PO Yetkisi)");
            poBtn.addClickListener(e -> {
                selectedRequestForPo = request;
                poDialog.open();
            });

            Button statusBtn = new Button(VaadinIcon.EXCHANGE.create());
            statusBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            statusBtn.getElement().setProperty("title", "Sistem Durumunu Değiştir");
            statusBtn.addClickListener(e -> {
                selectedRequestForStatus = request;
                statusCombo.setValue(request.getStatus());
                statusDialog.open();
            });

            actions.add(sendToPoBtn, poBtn, statusBtn, closeBtn);
            return actions;
        }).setHeader("Yönetici İşlemleri").setAutoWidth(true);

        grid.addComponentColumn(this::createChatButton).setHeader("Sohbet").setAutoWidth(true).setFlexGrow(0);
        
        grid.addComponentColumn(request -> {
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create());
            historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            historyBtn.addClickListener(e -> openHistoryDialog(request));
            return historyBtn;
        }).setHeader("Geçmiş").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(this::createScreenshotButton).setHeader("Görsel").setAutoWidth(true).setFlexGrow(0);

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(titleCol).setComponent(createFilterHeader("Başlık...", requestFilter::setTitle));
        headerRow.getCell(descCol).setComponent(createFilterHeader("Detay...", requestFilter::setDescription));
        headerRow.getCell(statusCol).setComponent(createFilterHeader("Durum...", requestFilter::setStatus));
        headerRow.getCell(dateCol).setComponent(createDateRangeFilterHeader(requestFilter));
    }

    // --- ADMİN EKRANI: MEMNUNİYET DEĞİŞTİRME/EKLEME METOTLARI ---
    private Component createRatingColumn(RequestEntity request) {
        if ("KAPATILDI".equals(request.getStatus())) {
            Button rateBtn = new Button();
            if (request.getSatisfactionScore() != null) {
                rateBtn.setText("⭐ " + request.getSatisfactionScore() + "/5");
                rateBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                if (request.getSatisfactionComment() != null && !request.getSatisfactionComment().isEmpty()) {
                    rateBtn.getElement().setProperty("title", "Yorum: " + request.getSatisfactionComment());
                }
            } else {
                rateBtn.setText("Puan Ver");
                rateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            }
            // Admin butona tıkladığında puanlama düzenleme penceresi açılır
            rateBtn.addClickListener(e -> openRatingEditDialog(request));
            return rateBtn;
        }
        return new Span("-");
    }

    private void openRatingEditDialog(RequestEntity request) {
        Dialog ratingDialog = new Dialog();
        ratingDialog.setHeaderTitle("Müşteri Memnuniyeti Düzenle (Admin Yetkisi)");
        ratingDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();

        RadioButtonGroup<Integer> scoreGroup = new RadioButtonGroup<>();
        scoreGroup.setLabel("Puan (1-5)");
        scoreGroup.setItems(1, 2, 3, 4, 5);
        scoreGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        if (request.getSatisfactionScore() != null) {
            scoreGroup.setValue(request.getSatisfactionScore());
        }
        
        TextArea commentArea = new TextArea("Yorum");
        commentArea.setWidthFull();
        if (request.getSatisfactionComment() != null) {
            commentArea.setValue(request.getSatisfactionComment());
        }

        layout.add(scoreGroup, commentArea);
        ratingDialog.add(layout);

        Button submitBtn = new Button("Kaydet", event -> {
            if (scoreGroup.getValue() == null) {
                Notification.show("Lütfen 1 ile 5 arasında bir puan seçin.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            request.setSatisfactionScore(scoreGroup.getValue());
            request.setSatisfactionComment(commentArea.getValue());
            requestRepository.save(request);

            String admin = SecurityContextHolder.getContext().getAuthentication().getName();
            systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebinin memnuniyet puanını güncelledi: " + scoreGroup.getValue() + " yıldız.");

            Notification.show("Puan başarıyla güncellendi!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            ratingDialog.close();
            dataView.refreshItem(request); 
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("İptal", e -> ratingDialog.close());

        ratingDialog.getFooter().add(submitBtn, cancelBtn);
        ratingDialog.open();
    }
    // --- ADMİN MEMNUNİYET METOTLARI BİTİŞ ---

    private Component createChatButton(RequestEntity request) {
        Button chatButton = new Button("Sohbet", VaadinIcon.CHAT.create());
        chatButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Div container = new Div(chatButton);
        container.getStyle().set("position", "relative").set("display", "inline-block");

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(currentUser -> {
            int unreadCount = chatService.getUnreadMessageCount(request.getRequestId(), currentUser.getUserId());
            if (unreadCount > 0) {
                Span badge = new Span(String.valueOf(unreadCount));
                badge.getElement().getThemeList().add("badge error primary pill");
                badge.getStyle().set("position", "absolute").set("top", "-5px").set("right", "-5px")
                        .set("padding", "2px 6px").set("font-size", "10px").set("font-weight", "bold");
                container.add(badge);
            }
        });

        chatButton.addClickListener(e -> {
            systemLogService.log("Admin (" + email + "), ID: " + request.getRequestId() + " olan talebin sohbetine giriş yaptı.");
            getUI().ifPresent(ui -> ui.navigate(TalepChat.class, request.getRequestId()));
        });
        return container;
    }

    private void configureCloseDialog() {
        closeDialog.setHeaderTitle("Talebi Kapat / Reddet");
        closeReason.setWidthFull();

        Button confirmBtn = new Button("Kapat", e -> {
            if (selectedRequestForClose != null) {
                selectedRequestForClose.setStatus("KAPATILDI");
                requestRepository.save(selectedRequestForClose);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForClose.getRequestId() + " talebini kapattı. Gerekçe: " + closeReason.getValue());
                Notification.show("Talep kapatıldı.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                closeDialog.close();
                closeReason.clear();
                refreshGrid();
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("İptal", e -> closeDialog.close());

        closeDialog.getFooter().add(confirmBtn, cancelBtn);
        closeDialog.add(closeReason);
    }

    private void forwardToPo(RequestEntity request) {
        request.setStatus("INCELEMEDE");
        requestRepository.save(request);
        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        systemLogService.log("Admin (" + admin + "), ID: " + request.getRequestId() + " talebini PO incelemesine sevk etti.");
        Notification.show("Talep PO havuzuna alındı.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }

    private void configureStatusDialog() {
        statusCombo.setItems("NEW", "INCELEMEDE", "ONAYLANDI", "İş Akışına Dönüştü", "KAPATILDI");
        statusCombo.setWidthFull();

        Button saveBtn = new Button("Kaydet", e -> {
            if (selectedRequestForStatus != null && statusCombo.getValue() != null) {
                selectedRequestForStatus.setStatus(statusCombo.getValue());
                requestRepository.save(selectedRequestForStatus);
                String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForStatus.getRequestId() + " talebinin sistem durumunu '" + statusCombo.getValue() + "' yaptı.");
                Notification.show("Durum güncellendi.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                statusDialog.close();
                refreshGrid();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("İptal", e -> statusDialog.close());

        statusDialog.add(statusCombo);
        statusDialog.getFooter().add(saveBtn, cancelBtn);
    }

    private void configurePoDialog() {
        impact.setItems("1 - Çok Düşük", "2 - Düşük", "3 - Orta", "4 - Yüksek", "5 - Kritik");
        urgency.setItems("1 - Çok Düşük", "2 - Düşük", "3 - Orta", "4 - Yüksek", "5 - Çok Acil");
        effort.setItems("1 - Çok Kısa", "2 - Kısa", "3 - Orta", "4 - Uzun", "5 - Çok Uzun");
        
        securityOverride.getStyle().set("margin-top", "15px").set("font-weight", "bold").set("color", "var(--lumo-error-text-color)");
        FormLayout fl = new FormLayout(impact, urgency, effort);
        fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button scoreBtn = new Button("Önceliklendir & Onayla", e -> {
            if (selectedRequestForPo != null) {
                boolean secOverride = securityOverride.getValue();
                if (!secOverride && (urgency.getValue() == null || impact.getValue() == null || effort.getValue() == null)) {
                    Notification.show("Değerleri seçin.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                try {
                    int urg = urgency.getValue() != null ? Character.getNumericValue(urgency.getValue().charAt(0)) : 5;
                    int imp = impact.getValue() != null ? Character.getNumericValue(impact.getValue().charAt(0)) : 5;
                    int eff = effort.getValue() != null ? Character.getNumericValue(effort.getValue().charAt(0)) : 1;
                    
                    requestService.prioritizeRequest(selectedRequestForPo.getRequestId(), urg, imp, eff, secOverride);
                    String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                    systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForPo.getRequestId() + " talebini önceliklendirip ONAYLANDI yaptı.");
                    Notification.show("Talep önceliklendirildi.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    poDialog.close();
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show("Hata: " + ex.getMessage(), 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        scoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button toWorkflowBtn = new Button("Göreve Dönüştür (Yazılım)", e -> {
            if (selectedRequestForPo != null) {
                try {
                    requestService.goreveDonustur(selectedRequestForPo);
                    String admin = SecurityContextHolder.getContext().getAuthentication().getName();
                    systemLogService.log("Admin (" + admin + "), ID: " + selectedRequestForPo.getRequestId() + " talebini YAZILIM GÖREVİNE dönüştürdü.");
                    Notification.show("Göreve dönüştürüldü.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    poDialog.close();
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        toWorkflowBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("İptal", e -> poDialog.close());

        poDialog.add(fl, securityOverride);
        poDialog.getFooter().add(scoreBtn, toWorkflowBtn, cancelBtn);
    }

    private void openHistoryDialog(RequestEntity request) {
        Dialog historyDialog = new Dialog();
        historyDialog.setHeaderTitle("Talep Geçmişi (Talep #" + request.getRequestId() + ")");
        historyDialog.setWidth("600px");
        historyDialog.setMaxHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        try {
            var logs = systemLogService.getLogsForRequest(request.getRequestId());
            if (logs == null || logs.isEmpty()) {
                layout.add(new Span("Kayıt bulunamadı."));
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                for (var log : logs) {
                    Div stepItem = new Div();
                    stepItem.getStyle().set("border-left", "3px solid var(--lumo-primary-color)").set("padding-left", "15px").set("margin-bottom", "15px");
                    String dateStr = (log.getCreatedAt() != null) ? log.getCreatedAt().format(formatter) : "Bilinmiyor";
                    Span dateSpan = new Span(dateStr);
                    dateSpan.getStyle().set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)").set("display", "block").set("font-weight", "bold");
                    Span actionSpan = new Span(log.getAction());
                    actionSpan.getStyle().set("display", "block").set("margin-top", "4px");
                    stepItem.add(dateSpan, actionSpan);
                    layout.add(stepItem);
                }
            }
        } catch (Exception e) {
            layout.add(new Span("Hata: " + e.getMessage()));
        }

        historyDialog.add(layout);
        historyDialog.getFooter().add(new Button("Kapat", e -> historyDialog.close()));
        historyDialog.open();
    }

    private Component createScreenshotButton(RequestEntity request) {
        boolean hasScreenshot = request.getScreenshotData() != null && request.getScreenshotData().length > 0;
        Button button = new Button(hasScreenshot ? "Bak" : "Yok", VaadinIcon.PICTURE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        if (!hasScreenshot) {
            button.setEnabled(false);
            return button;
        }
        button.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Görsel #" + request.getRequestId());
            StreamResource res = new StreamResource("img.png", () -> new ByteArrayInputStream(request.getScreenshotData()));
            Image img = new Image(res, "Görsel");
            img.setWidthFull();
            dialog.add(img);
            dialog.getFooter().add(new Button("Kapat", ev -> dialog.close()));
            dialog.open();
        });
        return button;
    }

    private void refreshGrid() {
        if (dataView == null) {
            dataView = grid.setItems(requestRepository.findAll());
            requestFilter.setDataView(dataView);
        } else {
            dataView.refreshAll();
        }
    }

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

    private static Component createFilterHeader(String placeholder, Consumer<String> filterChangeConsumer) {
        TextField tf = new TextField();
        tf.setPlaceholder(placeholder);
        tf.setValueChangeMode(ValueChangeMode.EAGER);
        tf.setClearButtonVisible(true);
        tf.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tf.setWidthFull();
        tf.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return tf;
    }

    private static Component createDateRangeFilterHeader(RequestFilter requestFilter) {
        VerticalLayout dateLayout = new VerticalLayout();
        dateLayout.setPadding(false); dateLayout.setSpacing(true);
        DateTimePicker startPicker = new DateTimePicker("Başlangıç");
        startPicker.setWidthFull();
        startPicker.addValueChangeListener(e -> requestFilter.setStartDate(e.getValue()));
        DateTimePicker endPicker = new DateTimePicker("Bitiş");
        endPicker.setWidthFull();
        endPicker.addValueChangeListener(e -> requestFilter.setEndDate(e.getValue()));
        dateLayout.add(startPicker, endPicker);
        return dateLayout;
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

        public void setTitle(String t) { this.title = t; refresh(); }
        public void setDescription(String d) { this.description = d; refresh(); }
        public void setStatus(String s) { this.status = s; refresh(); }
        public void setStartDate(LocalDateTime s) { this.startDate = s; refresh(); }
        public void setEndDate(LocalDateTime e) { this.endDate = e; refresh(); }

        private void refresh() { if (dataView != null) dataView.refreshAll(); }

        public boolean test(RequestEntity request) {
            boolean mTitle = matches(request.getTitle(), title);
            boolean mDesc = matches(request.getDescription(), description);
            boolean mStatus = matches(request.getStatus(), status);
            boolean mDate = true;
            if (request.getCreatedAt() != null) {
                if (startDate != null && request.getCreatedAt().isBefore(startDate)) mDate = false;
                if (endDate != null && request.getCreatedAt().isAfter(endDate)) mDate = false;
            }
            return mTitle && mDesc && mStatus && mDate;
        }

        private boolean matches(String val, String search) {
            if (search == null || search.isEmpty()) return true;
            return val != null && val.toLowerCase().contains(search.toLowerCase());
        }
    }
}