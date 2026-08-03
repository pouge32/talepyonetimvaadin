package com.example.base.ui.CustomerScreen;

import com.example.base.repository.UserRepository;
import com.example.base.service.FaqService; // EKLENDİ
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.accordion.Accordion; // EKLENDİ
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode; // EKLENDİ
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "talep-olustur", layout = MainLayout.class)
@RolesAllowed("CUSTOMER")
public class TalepAcma extends VerticalLayout {

    public enum Kategori {
        YAZILIM("Yazılım Hatası", "Uygulama veya ekranlarda karşılaştığınız hatalar"),
        DONANIM("Donanım", "Bilgisayar, yazıcı, ağ cihazı gibi fiziksel sorunlar"),
        AG("Ağ / Bağlantı", "İnternet, VPN veya sunucu erişim sorunları"),
        ERISIM("Erişim / Yetki Talebi", "Yeni yetki, şifre sıfırlama, hesap talepleri"),
        DIGER("Diğer", "Yukarıdakilere uymayan diğer talepleriniz");

        private final String label;
        private final String hint;

        Kategori(String label, String hint) {
            this.label = label;
            this.hint = hint;
        }

        public String getLabel() { return label; }
        public String getHint() { return hint; }
    }

    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final TextField title = new TextField("Talep Başlığı");
    private final ComboBox<Kategori> category = new ComboBox<>("Kategori");
    private final TextArea description = new TextArea("Talep Detayı");
    private final Span charCounter = new Span("0 / " + MAX_DESCRIPTION_LENGTH);
    private final Button submitButton = new Button("Talebi Gönder");
    private final Button clearButton = new Button("Temizle");

    private final MemoryBuffer uploadBuffer = new MemoryBuffer();
    private final Upload screenshotUpload = new Upload(uploadBuffer);
    private final VerticalLayout previewsListLayout = new VerticalLayout();
    
    private final List<byte[]> uploadedBytesList = new ArrayList<>();
    private final List<String> uploadedFileNames = new ArrayList<>();
    private final List<String> uploadedMimeTypes = new ArrayList<>();

    private final RequestService requestService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final FaqService faqService; // EKLENDİ

    // SSS (Bilgi Bankası) Bileşenleri
    private final Accordion faqAccordion = new Accordion();

    private final Binder<RequestFormDto> binder = new Binder<>(RequestFormDto.class);
    private RequestFormDto currentDto = new RequestFormDto();

    public TalepAcma(RequestService requestService, UserRepository userRepository,
                     NotificationService notificationService, SystemLogService systemLogService,
                     FaqService faqService) { // Constructor güncellendi
        this.requestService = requestService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;
        this.faqService = faqService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                  .set("overflow", "hidden");

        // Kaydırma (Scroll) işlemi için ana kapsayıcı
        Div scrollWrapper = new Div();
        scrollWrapper.setWidthFull();
        scrollWrapper.getStyle()
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("overflow-y", "auto")
                .set("padding-right", "8px");

        // SSS (Bilgi Bankası) Bölümü eklendi
        Div faqSection = buildFaqSection();
        faqSection.getStyle().set("margin-bottom", "20px");

        // İki kolonlu form bölümü
        Div gridContainer = new Div();
        gridContainer.setWidthFull();
        gridContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "2fr 1fr")
                .set("gap", "20px");

        VerticalLayout leftColumn = new VerticalLayout(buildFormCard());
        leftColumn.setPadding(false);
        leftColumn.setSpacing(false);

        VerticalLayout rightColumn = new VerticalLayout(buildTipsCard(), buildCategoryLegendCard());
        rightColumn.setPadding(false);
        rightColumn.setSpacing(true);

        gridContainer.add(leftColumn, rightColumn);
        scrollWrapper.add(faqSection, gridContainer); // Önce SSS, sonra Form

        add(buildPageHeader(), scrollWrapper);
    }

    private Div buildPageHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("margin-bottom", "15px").set("flex-shrink", "0");

        H2 heading = new H2("Yeni Talep Oluştur");
        heading.getStyle().set("margin", "0 0 2px 0").set("color", "var(--lumo-header-text-color)").set("font-size", "22px");

        Paragraph subtitle = new Paragraph("Yaşadığınız teknik problemi veya talebinizi detaylıca ileterek çözüm sürecini hızlandırın.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        header.add(heading, subtitle);
        return header;
    }

    // --- SSS / BİLGİ BANKASI BÖLÜMÜ BAŞLANGICI ---
    private Div buildFaqSection() {
        Div card = createCard();

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        Div badge = iconBadge(VaadinIcon.SEARCH, "var(--lumo-success-color)");
        H3 titleText = new H3("Nasıl Yardımcı Olabiliriz? (Bilgi Bankası)");
        titleText.getStyle().set("margin", "0").set("font-size", "16px");
        headerRow.add(badge, titleText);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Talep oluşturmadan önce sorununuzu burada aratın (Örn: şifre, fatura, erişim...)");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshFaq(e.getValue()));

        faqAccordion.setWidthFull();
        refreshFaq(""); // İlk açılışta tüm SSS'leri getir

        VerticalLayout layout = new VerticalLayout(headerRow, searchField, faqAccordion);
        layout.setPadding(false);
        layout.setSpacing(true);

        card.add(layout);
        return card;
    }

    private void refreshFaq(String keyword) {
        faqAccordion.getChildren().toList().forEach(faqAccordion::remove);

        var faqs = faqService.searchFaq(keyword);

        if (faqs.isEmpty()) {
            faqAccordion.add("Sonuç Bulunamadı", new Paragraph("Aradığınız kelimeyle ilgili çözüm bankamızda bir kayıt yok. Lütfen aşağıdan talep oluşturun."));
        } else {
            for (var faq : faqs) {
                faqAccordion.add(faq.getQuestion(), new Paragraph(faq.getAnswer()));
            }
        }
    }

    private Div createCard() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "14px")
                .set("box-shadow", "0 4px 15px rgba(0, 0, 0, 0.04)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-sizing", "border-box")
                .set("padding", "20px");
        return card;
    }

    private Div iconBadge(VaadinIcon vaadinIcon, String colorVar) {
        Div badge = new Div();
        Icon icon = vaadinIcon.create();
        icon.setSize("18px");
        icon.getStyle().set("color", colorVar);
        badge.getStyle()
                .set("background-color", "color-mix(in srgb, " + colorVar + " 12%, white)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "36px")
                .set("height", "36px")
                .set("border-radius", "10px")
                .set("flex-shrink", "0");
        badge.add(icon);
        return badge;
    }

    private Div buildScreenshotSection() {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.getStyle().set("margin-top", "16px");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(true);
        Icon camIcon = VaadinIcon.CAMERA.create();
        camIcon.setSize("16px");
        camIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span label = new Span("Ekran Görüntüleri (Birden fazla ekleyebilirsiniz)");
        label.getStyle().set("font-weight", "500").set("font-size", "var(--lumo-font-size-s)");
        labelRow.add(camIcon, label);

        screenshotUpload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        screenshotUpload.setMaxFiles(10); 
        screenshotUpload.setMaxFileSize((int) MAX_FILE_SIZE_BYTES);
        screenshotUpload.setDropAllowed(true);
        screenshotUpload.setWidthFull();
        screenshotUpload.getStyle()
                .set("margin-top", "8px")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border", "1px dashed var(--lumo-contrast-20pct)")
                .set("border-radius", "10px")
                .set("padding", "4px");
        screenshotUpload.setUploadButton(new Button("Görsel Seç", VaadinIcon.UPLOAD.create()));

        Span dropLabel = new Span("veya dosyaları buraya sürükleyin (PNG, JPG, WEBP — her biri en fazla 5MB)");
        dropLabel.getStyle()
                .set("color", "var(--lumo-body-text-color) !important")
                .set("-webkit-text-fill-color", "var(--lumo-body-text-color)")
                .set("background", "transparent")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("user-select", "none");
        screenshotUpload.setDropLabel(dropLabel);

        screenshotUpload.addSucceededListener(event -> {
            try (InputStream inputStream = uploadBuffer.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int read;
                while ((read = inputStream.read(data)) != -1) {
                    buffer.write(data, 0, read);
                }
                byte[] fileBytes = buffer.toByteArray();
                String fileName = event.getFileName();
                String mimeType = event.getMIMEType();

                uploadedBytesList.add(fileBytes);
                uploadedFileNames.add(fileName);
                uploadedMimeTypes.add(mimeType);

                renderPreviews();

                Notification.show("Görsel eklendi: " + fileName, 2000, Notification.Position.TOP_CENTER);
            } catch (IOException e) {
                Notification.show("Görsel okunurken hata: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            }
        });

        screenshotUpload.addFileRejectedListener(event -> {
            Notification error = Notification.show(event.getErrorMessage(), 4000, Notification.Position.TOP_CENTER);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        previewsListLayout.setPadding(false);
        previewsListLayout.setSpacing(true);
        previewsListLayout.setWidthFull();
        previewsListLayout.getStyle().set("margin-top", "10px");

        wrapper.add(labelRow, screenshotUpload, previewsListLayout);
        return wrapper;
    }

    private void renderPreviews() {
        previewsListLayout.removeAll();
        for (int i = 0; i < uploadedFileNames.size(); i++) {
            final int index = i;
            Div itemCard = new Div();
            itemCard.setWidthFull();
            itemCard.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "space-between")
                    .set("background", "var(--lumo-base-color)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "8px")
                    .set("padding", "8px 12px");

            HorizontalLayout info = new HorizontalLayout();
            info.setAlignItems(FlexComponent.Alignment.CENTER);
            info.setSpacing(true);

            Icon imgIcon = VaadinIcon.FILE_PICTURE.create();
            imgIcon.getStyle().set("color", "var(--lumo-primary-color)");
            Span nameSpan = new Span(uploadedFileNames.get(i));
            nameSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("font-weight", "500");
            info.add(imgIcon, nameSpan);

            Button removeBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                uploadedBytesList.remove(index);
                uploadedFileNames.remove(index);
                uploadedMimeTypes.remove(index);
                renderPreviews();
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            itemCard.add(info, removeBtn);
            previewsListLayout.add(itemCard);
        }
    }

    private void clearScreenshots() {
        uploadedBytesList.clear();
        uploadedFileNames.clear();
        uploadedMimeTypes.clear();
        screenshotUpload.clearFileList();
        previewsListLayout.removeAll();
    }

    private Div buildFormCard() {
        Div card = createCard();

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        headerRow.setWidthFull();

        Div badge = iconBadge(VaadinIcon.CLIPBOARD_TEXT, "var(--lumo-primary-color)");

        VerticalLayout headerText = new VerticalLayout();
        headerText.setPadding(false);
        headerText.setSpacing(false);
        H3 cardTitle = new H3("Talep Formu");
        cardTitle.getStyle().set("margin", "0").set("font-size", "16px");
        Span requiredNote = new Span("* işaretli alanların doldurulması zorunludur");
        requiredNote.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)");
        headerText.add(cardTitle, requiredNote);

        headerRow.add(badge, headerText);

        title.setWidthFull();
        title.setPlaceholder("Örn: VPN bağlantısı sağlanamıyor");
        title.setClearButtonVisible(true);

        category.setWidthFull();
        category.setPlaceholder("Kategori seçiniz");
        category.setItems(Kategori.values());
        category.setItemLabelGenerator(Kategori::getLabel);

        description.setWidthFull();
        description.setPlaceholder("Karşılaştığınız sorunu ve aldığınız hata kodlarını buraya yazın...");
        description.setMinHeight("140px");
        description.setMaxLength(MAX_DESCRIPTION_LENGTH);
        description.addValueChangeListener(e -> {
            int len = e.getValue() == null ? 0 : e.getValue().length();
            charCounter.setText(len + " / " + MAX_DESCRIPTION_LENGTH);
        });

        charCounter.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("display", "block")
                .set("text-align", "right")
                .set("margin-top", "4px");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(title, category);
        formLayout.add(description);
        formLayout.setColspan(description, 2);
        formLayout.getStyle().set("margin-top", "12px");

        setupBinder();

        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.getStyle().set("height", "40px").set("font-weight", "600");
        submitButton.addClickListener(event -> saveRequest());

        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.getStyle().set("height", "40px");
        clearButton.addClickListener(event -> resetForm());

        HorizontalLayout actions = new HorizontalLayout(submitButton, clearButton);
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "16px");
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(headerRow, formLayout, buildScreenshotSection(), charCounter, actions);
        content.setPadding(false);
        content.setSpacing(false);

        card.add(content);
        return card;
    }

    private Div buildTipsCard() {
        Div card = createCard();

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        Div badge = iconBadge(VaadinIcon.LIGHTBULB, "var(--lumo-success-color)");
        H3 title2 = new H3("Hızlı İpuçları");
        title2.getStyle().set("margin", "0").set("font-size", "15px");
        headerRow.add(badge, title2);

        VerticalLayout tips = new VerticalLayout();
        tips.setPadding(false);
        tips.setSpacing(true);
        tips.getStyle().set("margin-top", "12px");

        String[] tipTexts = {
                "Başlığı kısa ve net tutun.",
                "Birden fazla hata ekran görüntüsü ekleyebilirsiniz.",
                "Doğru kategori seçimi süreci hızlandırır."
        };

        for (String t : tipTexts) {
            HorizontalLayout row = new HorizontalLayout();
            row.setSpacing(true);
            row.setAlignItems(FlexComponent.Alignment.START);
            Icon dot = VaadinIcon.CHECK_CIRCLE.create();
            dot.setSize("14px");
            dot.getStyle().set("color", "var(--lumo-success-color)").set("margin-top", "3px").set("flex-shrink", "0");
            Span text = new Span(t);
            text.getStyle().set("color", "var(--lumo-body-text-color)").set("font-size", "var(--lumo-font-size-xs)");
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
        H3 title2 = new H3("Kategori Rehberi");
        title2.getStyle().set("margin", "0").set("font-size", "15px");
        headerRow.add(badge, title2);

        VerticalLayout legend = new VerticalLayout();
        legend.setPadding(false);
        legend.setSpacing(false);
        legend.getStyle().set("margin-top", "12px");

        for (Kategori k : Kategori.values()) {
            Div row = new Div();
            row.getStyle()
                    .set("padding", "6px 0")
                    .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");
            Span name = new Span(k.getLabel());
            name.getStyle().set("font-weight", "600").set("display", "block").set("font-size", "var(--lumo-font-size-xs)");
            Span hint = new Span(k.getHint());
            hint.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "11px")
                    .set("display", "block")
                    .set("margin-top", "1px");
            row.add(name, hint);
            legend.add(row);
        }

        card.add(headerRow, legend);
        return card;
    }

    private void setupBinder() {
        binder.forField(title)
                .asRequired("Başlık boş olamaz")
                .withValidator(t -> t.trim().length() >= 5, "Başlık en az 5 karakter olmalıdır")
                .bind(RequestFormDto::getTitle, RequestFormDto::setTitle);

        binder.forField(category)
                .asRequired("Lütfen bir kategori seçin")
                .bind(RequestFormDto::getCategory, RequestFormDto::setCategory);

        binder.forField(description)
                .asRequired("Detay boş olamaz")
                .withValidator(d -> d.trim().length() >= 10, "Açıklama en az 10 karakter olmalıdır")
                .bind(RequestFormDto::getDescription, RequestFormDto::setDescription);

        binder.setBean(currentDto);
    }

    private void resetForm() {
        currentDto = new RequestFormDto();
        binder.setBean(currentDto);
        charCounter.setText("0 / " + MAX_DESCRIPTION_LENGTH);
        clearScreenshots();
    }

    private void saveRequest() {
        if (binder.isValid()) {
            try {
                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                String loggedInEmail = (auth != null) ? auth.getName() : "";

                com.example.base.entity.UserEntity customerUser = userRepository.findByEmail(loggedInEmail)
                        .orElseThrow(() -> new RuntimeException("Giriş yapan kullanıcı bulunamadı!"));

                byte[] mainBytes = !uploadedBytesList.isEmpty() ? uploadedBytesList.get(0) : null;
                String mainName = !uploadedFileNames.isEmpty() ? uploadedFileNames.get(0) : null;
                String mainMime = !uploadedMimeTypes.isEmpty() ? uploadedMimeTypes.get(0) : null;

                requestService.createRequest(
                        customerUser.getUserId(),
                        currentDto.getTitle(),
                        currentDto.getDescription(),
                        currentDto.getCategory().name(),
                        mainBytes,
                        mainName,
                        mainMime
                );

                String ekGoruntusuNotu = !uploadedFileNames.isEmpty()
                        ? " [Ekler: " + String.join(", ", uploadedFileNames) + "]"
                        : "";

                systemLogService.log("Müşteri (" + loggedInEmail + ") yeni talep oluşturdu: "
                        + currentDto.getTitle() + " [Kategori: " + currentDto.getCategory().getLabel() + "]" + ekGoruntusuNotu);

                notificationService.notifyRole("PO", "Yeni Talep Geldi",
                        "İncelenmesi gereken yeni bir talep eklendi: " + currentDto.getTitle());

                Notification success = Notification.show("Talebiniz başarıyla oluşturuldu!", 3000,
                        Notification.Position.TOP_CENTER);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                resetForm();

            } catch (Exception e) {
                Notification error = Notification.show("Hata oluştu: " + e.getMessage(), 4000,
                        Notification.Position.TOP_CENTER);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            binder.validate();
            Notification.show("Lütfen formdaki hatalı alanları düzeltin.", 3000, Notification.Position.MIDDLE);
        }
    }

    public static class RequestFormDto {
        private String title;
        private String description;
        private Kategori category;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Kategori getCategory() { return category; }
        public void setCategory(Kategori category) { this.category = category; }
    }
}