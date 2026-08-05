package com.example.base.ui.CustomerScreen;

import com.example.base.repository.UserRepository;
import com.example.base.service.FaqService;
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "talep-olustur", layout = MainLayout.class)
@RolesAllowed("CUSTOMER")
public class TalepAcma extends VerticalLayout implements HasDynamicTitle {

    public enum Kategori {
        YAZILIM("Yazılım Hatası", "Uygulama veya ekranlarda karşılaştığınız hatalar", "Software Error", "Errors you encounter in applications or screens"),
        DONANIM("Donanım", "Bilgisayar, yazıcı, ağ cihazı gibi fiziksel sorunlar", "Hardware", "Physical problems like computers, printers, network devices"),
        AG("Ağ / Bağlantı", "İnternet, VPN veya sunucu erişim sorunları", "Network / Connection", "Internet, VPN or server access issues"),
        ERISIM("Erişim / Yetki Talebi", "Yeni yetki, şifre sıfırlama, hesap talepleri", "Access / Authorization Request", "New permissions, password resets, account requests"),
        DIGER("Diğer", "Yukarıdakilere uymayan diğer talepleriniz", "Other", "Other requests that do not fit above");

        private final String labelTr;
        private final String hintTr;
        private final String labelEn;
        private final String hintEn;

        Kategori(String labelTr, String hintTr, String labelEn, String hintEn) {
            this.labelTr = labelTr;
            this.hintTr = hintTr;
            this.labelEn = labelEn;
            this.hintEn = hintEn;
        }

        public String getLabel() { 
            boolean isEnglish = "en".equals(UI.getCurrent().getLocale().getLanguage());
            return isEnglish ? labelEn : labelTr; 
        }

        public String getHint() { 
            boolean isEnglish = "en".equals(UI.getCurrent().getLocale().getLanguage());
            return isEnglish ? hintEn : hintTr; 
        }
    }

    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final TextField title = new TextField();
    private final ComboBox<Kategori> category = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final Span charCounter = new Span("0 / " + MAX_DESCRIPTION_LENGTH);
    private final Button submitButton = new Button();
    private final Button clearButton = new Button();

    private final MemoryBuffer uploadBuffer = new MemoryBuffer();
    private final Upload screenshotUpload = new Upload(uploadBuffer);
    private final VerticalLayout previewsListLayout = new VerticalLayout();
    
    private final Div faqSuggestionBox = new Div();
    private final Span suggestionText = new Span();
    private final Paragraph suggestionAnswer = new Paragraph();
    
    private final List<byte[]> uploadedBytesList = new ArrayList<>();
    private final List<String> uploadedFileNames = new ArrayList<>();
    private final List<String> uploadedMimeTypes = new ArrayList<>();

    private final RequestService requestService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final FaqService faqService;

    private final Binder<RequestFormDto> binder = new Binder<>(RequestFormDto.class);
    private RequestFormDto currentDto = new RequestFormDto();

    public TalepAcma(RequestService requestService, UserRepository userRepository,
                     NotificationService notificationService, SystemLogService systemLogService,
                     FaqService faqService) {
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

        Div scrollWrapper = new Div();
        scrollWrapper.setWidthFull();
        scrollWrapper.getStyle()
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("overflow-y", "auto")
                .set("padding-right", "8px");

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
        scrollWrapper.add(gridContainer);

        add(buildPageHeader(), scrollWrapper);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("request.create.pageTitle");
    }

    private Div buildPageHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle()
                .set("margin-bottom", "15px")
                .set("flex-shrink", "0")
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        Div titleArea = new Div();
        H2 heading = new H2(getTranslation("request.create.heading"));
        heading.getStyle().set("margin", "0 0 2px 0").set("color", "var(--lumo-header-text-color)").set("font-size", "22px");
        Paragraph subtitle = new Paragraph(getTranslation("request.create.subtitle"));
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        titleArea.add(heading, subtitle);

        Button faqButton = new Button(getTranslation("request.create.faqBtn"), VaadinIcon.QUESTION_CIRCLE.create());
        faqButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        faqButton.getStyle()
                .set("cursor", "pointer")
                .set("font-weight", "bold");
        faqButton.addClickListener(e -> UI.getCurrent().navigate(FaqView.class));

        header.add(titleArea, faqButton);
        return header;
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
        Span label = new Span(getTranslation("request.create.screenshotsLabel"));
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
                .set("padding", "4px")
                .set("--lumo-primary-color", "#333333")  
                .set("--lumo-primary-text-color", "#333333")
                .set("color", "#333333");

        screenshotUpload.setUploadButton(new Button(getTranslation("request.create.uploadBtn"), VaadinIcon.UPLOAD.create()));

        Span dropLabel = new Span(getTranslation("request.create.dropLabel"));
        dropLabel.getStyle()
                .set("color", "#333333 !important") 
                .set("-webkit-text-fill-color", "#333333") 
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

                Notification.show(getTranslation("request.create.notif.added") + fileName, 2000, Notification.Position.TOP_CENTER);
            } catch (IOException e) {
                Notification.show(getTranslation("request.create.notif.readError") + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
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
        faqSuggestionBox.setVisible(false);
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
        H3 cardTitle = new H3(getTranslation("request.create.formTitle"));
        cardTitle.getStyle().set("margin", "0").set("font-size", "16px");
        Span requiredNote = new Span(getTranslation("request.create.requiredNote"));
        requiredNote.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)");
        headerText.add(cardTitle, requiredNote);

        headerRow.add(badge, headerText);

        title.setLabel(getTranslation("request.create.field.title"));
        title.setWidthFull();
        title.setPlaceholder(getTranslation("request.create.field.titlePlaceholder"));
        title.setClearButtonVisible(true);

        category.setLabel(getTranslation("request.create.field.category"));
        category.setWidthFull();
        category.setPlaceholder(getTranslation("request.create.field.categoryPlaceholder"));
        category.setItems(Kategori.values());
        category.setItemLabelGenerator(Kategori::getLabel);

        description.setLabel(getTranslation("request.create.field.description"));
        description.setWidthFull();
        description.setPlaceholder(getTranslation("request.create.field.descPlaceholder"));
        description.setMinHeight("140px");
        description.setMaxLength(MAX_DESCRIPTION_LENGTH);

        description.setValueChangeMode(ValueChangeMode.LAZY);
        description.setValueChangeTimeout(1000); 
        description.addValueChangeListener(e -> {
            int len = e.getValue() == null ? 0 : e.getValue().length();
            charCounter.setText(len + " / " + MAX_DESCRIPTION_LENGTH);
            
            checkForFaqSuggestion(e.getValue());
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

        setupSuggestionBox();
        formLayout.add(faqSuggestionBox);
        formLayout.setColspan(faqSuggestionBox, 2);
        
        formLayout.getStyle().set("margin-top", "12px");

        setupBinder();

        submitButton.setText(getTranslation("request.create.submitBtn"));
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.getStyle().set("height", "40px").set("font-weight", "600");
        submitButton.addClickListener(event -> saveRequest()); 

        clearButton.setText(getTranslation("request.create.clearBtn"));
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
        H3 title2 = new H3(getTranslation("request.create.tips.title"));
        title2.getStyle().set("margin", "0").set("font-size", "15px");
        headerRow.add(badge, title2);

        VerticalLayout tips = new VerticalLayout();
        tips.setPadding(false);
        tips.setSpacing(true);
        tips.getStyle().set("margin-top", "12px");

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
        H3 title2 = new H3(getTranslation("request.create.legend.title"));
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
                .asRequired(getTranslation("request.create.validator.titleRequired"))
                .withValidator(t -> t.trim().length() >= 5, getTranslation("request.create.validator.titleMin"))
                .bind(RequestFormDto::getTitle, RequestFormDto::setTitle);

        binder.forField(category)
                .asRequired(getTranslation("request.create.validator.categoryRequired"))
                .bind(RequestFormDto::getCategory, RequestFormDto::setCategory);

        binder.forField(description)
                .asRequired(getTranslation("request.create.validator.descRequired"))
                .withValidator(d -> d.trim().length() >= 10, getTranslation("request.create.validator.descMin"))
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
            String formTitle = currentDto.getTitle();
            String formDesc = currentDto.getDescription();

            boolean isDuplicate = requestService.hasSimilarOpenRequest(formTitle, formDesc);

            if (isDuplicate) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader(getTranslation("request.create.dialog.header"));
                dialog.setText(getTranslation("request.create.dialog.text"));
                
                dialog.setCancelable(true);
                dialog.setCancelText(getTranslation("request.create.dialog.cancel"));
                dialog.setCancelButtonTheme("tertiary");
                
                dialog.setConfirmText(getTranslation("request.create.dialog.confirm"));
                dialog.setConfirmButtonTheme("primary error");

                dialog.addConfirmListener(event -> executeSaveToDatabase());
                dialog.open();
            } else {
                executeSaveToDatabase();
            }
        } else {
            binder.validate();
            Notification.show(getTranslation("request.create.validator.fixErrors"), 3000, Notification.Position.MIDDLE);
        }
    }

    private void executeSaveToDatabase() {
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

            Notification success = Notification.show(getTranslation("request.create.notif.success"), 3000,
                    Notification.Position.TOP_CENTER);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            resetForm();

        } catch (Exception e) {
            Notification error = Notification.show(getTranslation("request.create.notif.error") + e.getMessage(), 4000,
                    Notification.Position.TOP_CENTER);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void setupSuggestionBox() {
        faqSuggestionBox.setVisible(false);
        faqSuggestionBox.getStyle()
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("border", "1px solid var(--lumo-primary-color-50pct)")
                .set("border-radius", "8px")
                .set("padding", "15px")
                .set("margin-top", "10px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("transition", "all 0.3s ease-in-out");

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon ideaIcon = VaadinIcon.LIGHTBULB.create();
        ideaIcon.setColor("var(--lumo-primary-color)");
        
        Span headerText = new Span(getTranslation("request.create.suggestion.header"));
        headerText.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-text-color)");
        headerRow.add(ideaIcon, headerText);

        suggestionText.getStyle().set("font-weight", "600").set("font-size", "14px");
        suggestionAnswer.getStyle().set("margin", "0").set("font-size", "13px").set("color", "var(--lumo-secondary-text-color)");

        faqSuggestionBox.add(headerRow, suggestionText, suggestionAnswer);
    }

    private void checkForFaqSuggestion(String text) {
        if (text == null || text.trim().length() < 10) {
            faqSuggestionBox.setVisible(false);
            return;
        }

        List<String> userWords = extractMeaningfulWords(text);
        if (userWords.isEmpty()) {
            faqSuggestionBox.setVisible(false);
            return;
        }

        var allFaqs = faqService.searchFaq(""); 
        boolean isEnglish = "en".equals(UI.getCurrent().getLocale().getLanguage());
        
        int maxScore = 0;
        int bestIndex = -1; 

        for (int i = 0; i < allFaqs.size(); i++) {
            var faq = allFaqs.get(i);
            
            String q = isEnglish && faq.getQuestionEn() != null ? faq.getQuestionEn() : faq.getQuestion();
            String a = isEnglish && faq.getAnswerEn() != null ? faq.getAnswerEn() : faq.getAnswer();

            List<String> faqWords = extractMeaningfulWords(q + " " + a);
            
            int score = calculateMatchScore(userWords, faqWords);
            
            if (score > maxScore) {
                maxScore = score;
                bestIndex = i;
            }
        }

        if (bestIndex != -1 && maxScore > 0) {
            var topFaq = allFaqs.get(bestIndex); 
            
            String qDisplay = isEnglish && topFaq.getQuestionEn() != null ? topFaq.getQuestionEn() : topFaq.getQuestion();
            String aDisplay = isEnglish && topFaq.getAnswerEn() != null ? topFaq.getAnswerEn() : topFaq.getAnswer();

            suggestionText.setText(getTranslation("request.create.suggestion.question") + ": " + qDisplay);
            suggestionAnswer.setText(getTranslation("request.create.suggestion.answer") + ": " + aDisplay);
            faqSuggestionBox.setVisible(true);
        } else {
            faqSuggestionBox.setVisible(false);
        }
    }

    private List<String> extractMeaningfulWords(String text) {
        List<String> stopWords = List.of(
            "şirket", "şirkete", "şirketi", "nasıl", "neden", "niçin", "kim", 
            "hangi", "yapabilirim", "edebilirim", "istiyorum", "alabilirim", 
            "yardım", "lütfen", "için", "gibi", "kadar", "olan", "bana", 
            "benim", "bizim", "bunu", "veya", "ile", "göre", "acaba",
            "company", "how", "why", "who", "which", "want", "please", 
            "for", "like", "with", "about", "what", "can", "do"
        );

        return java.util.Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3 && !stopWords.contains(w))
                .toList();
    }

    private int calculateMatchScore(List<String> userWords, List<String> faqWords) {
        if (userWords.isEmpty() || faqWords.isEmpty()) return 0;
        
        java.util.Set<String> faqSet = new java.util.HashSet<>(faqWords);
        int matchCount = 0;
        
        for (String w : userWords) {
            if (faqSet.contains(w)) {
                matchCount++;
            }
        }
        return matchCount; 
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