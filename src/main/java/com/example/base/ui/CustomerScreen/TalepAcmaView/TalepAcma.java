package com.example.base.ui.CustomerScreen.TalepAcmaView;

import com.example.base.repository.UserRepository;
import com.example.base.service.FaqService;
import com.example.base.service.NotificationService;
import com.example.base.service.RequestService;
import com.example.base.service.SettingsService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.CustomerScreen.FaqView;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-olustur", layout = MainLayout.class)
@RolesAllowed({"CUSTOMER", "GODPANEL"})
@CssImport("./styles/customer/talep-acma.css")
public class TalepAcma extends VerticalLayout implements HasDynamicTitle {

    public enum Kategori {
        YAZILIM("Yazılım Hatası", "Uygulama veya ekranlarda karşılaştığınız hatalar", "Software Error", "Errors you encounter in applications or screens"),
        DONANIM("Donanım", "Bilgisayar, yazıcı, ağ cihazı gibi fiziksel sorunlar", "Hardware", "Physical problems like computers, printers, network devices"),
        AG("Ağ / Bağlantı", "İnternet, VPN veya sunucu erişim sorunları", "Network / Connection", "Internet, VPN or server access issues"),
        ERISIM("Erişim / Yetki Talebi", "Yeni yetki, şifre sıfırlama, hesap talepleri", "Access / Authorization Request", "New permissions, password resets, account requests"),
        DIGER("Diğer", "Yukarıdakilere uymayan diğer talepleriniz", "Other", "Other requests that do not fit above");

        private final String labelTr, hintTr, labelEn, hintEn;
        Kategori(String labelTr, String hintTr, String labelEn, String hintEn) {
            this.labelTr = labelTr; this.hintTr = hintTr; this.labelEn = labelEn; this.hintEn = hintEn;
        }
        public String getLabel() { return "en".equals(UI.getCurrent().getLocale().getLanguage()) ? labelEn : labelTr; }
        public String getHint() { return "en".equals(UI.getCurrent().getLocale().getLanguage()) ? hintEn : hintTr; }
    }

    public static class RequestFormDto {
        private String title; private String description; private Kategori category;
        public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
        public Kategori getCategory() { return category; } public void setCategory(Kategori category) { this.category = category; }
    }

    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final TextField title = new TextField();
    private final ComboBox<Kategori> category = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final Span charCounter = new Span("0 / " + MAX_DESCRIPTION_LENGTH);

    private final RequestService requestService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;

    private final Binder<RequestFormDto> binder = new Binder<>(RequestFormDto.class);
    private RequestFormDto currentDto = new RequestFormDto();

    private final TalepAcmaUploadComponent uploadComponent;
    private final TalepAcmaFaqComponent faqSuggestionBox;

    public TalepAcma(RequestService requestService, UserRepository userRepository,
                     NotificationService notificationService, SystemLogService systemLogService,
                     FaqService faqService, SettingsService settingsService) {
        this.requestService = requestService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;

        this.uploadComponent = new TalepAcmaUploadComponent(settingsService);
        this.faqSuggestionBox = new TalepAcmaFaqComponent(faqService);

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("talep-acma-layout");

        Div scrollWrapper = new Div();
        scrollWrapper.setWidthFull();
        scrollWrapper.addClassName("talep-acma-scroll-wrapper");

        Div gridContainer = new Div();
        gridContainer.setWidthFull();
        gridContainer.addClassName("talep-acma-grid-container");

        VerticalLayout leftColumn = new VerticalLayout(buildFormCard());
        leftColumn.setPadding(false); leftColumn.setSpacing(false);

        TalepAcmaRightPanelComponent rightColumn = new TalepAcmaRightPanelComponent();

        gridContainer.add(leftColumn, rightColumn);
        scrollWrapper.add(gridContainer);
        add(buildPageHeader(), scrollWrapper);
    }

    @Override
    public String getPageTitle() { return getTranslation("request.create.pageTitle"); }

    private Div buildPageHeader() {
        Div header = new Div(); header.setWidthFull(); header.addClassName("talep-acma-header");
        Div titleArea = new Div();
        H2 heading = new H2(getTranslation("request.create.heading")); heading.addClassName("talep-acma-heading");
        Paragraph subtitle = new Paragraph(getTranslation("request.create.subtitle")); subtitle.addClassName("talep-acma-subtitle");
        titleArea.add(heading, subtitle);

        Button faqButton = new Button(getTranslation("request.create.faqBtn"), VaadinIcon.QUESTION_CIRCLE.create());
        faqButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        faqButton.addClassName("talep-acma-faq-button");
        faqButton.addClickListener(e -> UI.getCurrent().navigate(FaqView.class));
        header.add(titleArea, faqButton); return header;
    }

    private Div buildFormCard() {
        Div card = new Div(); card.setWidthFull(); card.addClassName("talep-acma-card");
        HorizontalLayout headerRow = new HorizontalLayout(); headerRow.setAlignItems(FlexComponent.Alignment.CENTER); headerRow.setSpacing(true); headerRow.setWidthFull();

        Div badge = new Div(VaadinIcon.CLIPBOARD_TEXT.create());
        badge.getChildren().forEach(c -> ((Icon)c).setSize("18px"));
        badge.getChildren().forEach(c -> ((Icon)c).getStyle().set("color", "var(--lumo-primary-color)"));
        badge.addClassName("talep-acma-icon-badge");
        badge.getStyle().set("background-color", "color-mix(in srgb, var(--lumo-primary-color) 12%, white)");

        VerticalLayout headerText = new VerticalLayout(); headerText.setPadding(false); headerText.setSpacing(false);
        H3 cardTitle = new H3(getTranslation("request.create.formTitle")); cardTitle.addClassName("talep-acma-card-title");
        Span requiredNote = new Span(getTranslation("request.create.requiredNote")); requiredNote.addClassName("talep-acma-required-note");
        headerText.add(cardTitle, requiredNote);
        headerRow.add(badge, headerText);

        title.setLabel(getTranslation("request.create.field.title")); title.setWidthFull(); title.setPlaceholder(getTranslation("request.create.field.titlePlaceholder")); title.setClearButtonVisible(true);
        category.setLabel(getTranslation("request.create.field.category")); category.setWidthFull(); category.setPlaceholder(getTranslation("request.create.field.categoryPlaceholder")); category.setItems(Kategori.values()); category.setItemLabelGenerator(Kategori::getLabel);
        
        description.setLabel(getTranslation("request.create.field.description")); description.setWidthFull(); description.setPlaceholder(getTranslation("request.create.field.descPlaceholder")); description.setMinHeight("140px"); description.setMaxLength(MAX_DESCRIPTION_LENGTH);
        description.setValueChangeMode(ValueChangeMode.LAZY); description.setValueChangeTimeout(1000); 
        description.addValueChangeListener(e -> {
            int len = e.getValue() == null ? 0 : e.getValue().length();
            charCounter.setText(len + " / " + MAX_DESCRIPTION_LENGTH);
            faqSuggestionBox.checkForFaqSuggestion(e.getValue());
        });
        charCounter.addClassName("talep-acma-char-counter");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
        formLayout.add(title, category, description); formLayout.setColspan(description, 2);
        formLayout.add(faqSuggestionBox); formLayout.setColspan(faqSuggestionBox, 2);
        formLayout.addClassName("talep-acma-form-layout");

        setupBinder();

        Button submitBtn = new Button(getTranslation("request.create.submitBtn"), event -> saveRequest());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY); submitBtn.addClassName("talep-acma-submit-btn");

        Button clearBtn = new Button(getTranslation("request.create.clearBtn"), event -> resetForm());
        clearBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY); clearBtn.addClassName("talep-acma-clear-btn");

        HorizontalLayout actions = new HorizontalLayout(submitBtn, clearBtn);
        actions.setWidthFull(); actions.setSpacing(true); actions.addClassName("talep-acma-actions"); actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(headerRow, formLayout, uploadComponent, charCounter, actions);
        content.setPadding(false); content.setSpacing(false);
        card.add(content); return card;
    }

    private void setupBinder() {
        binder.forField(title).asRequired(getTranslation("request.create.validator.titleRequired")).withValidator(t -> t.trim().length() >= 5, getTranslation("request.create.validator.titleMin")).bind(RequestFormDto::getTitle, RequestFormDto::setTitle);
        binder.forField(category).asRequired(getTranslation("request.create.validator.categoryRequired")).bind(RequestFormDto::getCategory, RequestFormDto::setCategory);
        binder.forField(description).asRequired(getTranslation("request.create.validator.descRequired")).withValidator(d -> d.trim().length() >= 10, getTranslation("request.create.validator.descMin")).bind(RequestFormDto::getDescription, RequestFormDto::setDescription);
        binder.setBean(currentDto);
    }

    private void resetForm() {
        currentDto = new RequestFormDto();
        binder.setBean(currentDto);
        charCounter.setText("0 / " + MAX_DESCRIPTION_LENGTH);
        uploadComponent.clearScreenshots();
        faqSuggestionBox.setVisible(false);
    }

    private void saveRequest() {
        if (binder.isValid()) {
            boolean isDuplicate = requestService.hasSimilarOpenRequest(currentDto.getTitle(), currentDto.getDescription());
            if (isDuplicate) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader(getTranslation("request.create.dialog.header"));
                dialog.setText(getTranslation("request.create.dialog.text"));
                dialog.setCancelable(true); dialog.setCancelText(getTranslation("request.create.dialog.cancel")); dialog.setCancelButtonTheme("tertiary");
                dialog.setConfirmText(getTranslation("request.create.dialog.confirm")); dialog.setConfirmButtonTheme("primary error");
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
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String loggedInEmail = (auth != null) ? auth.getName() : "";
            com.example.base.entity.UserEntity customerUser = userRepository.findByEmail(loggedInEmail).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

            byte[] mainBytes = !uploadComponent.getUploadedBytesList().isEmpty() ? uploadComponent.getUploadedBytesList().get(0) : null;
            String mainName = !uploadComponent.getUploadedFileNames().isEmpty() ? uploadComponent.getUploadedFileNames().get(0) : null;
            String mainMime = !uploadComponent.getUploadedMimeTypes().isEmpty() ? uploadComponent.getUploadedMimeTypes().get(0) : null;

            requestService.createRequest(customerUser.getUserId(), currentDto.getTitle(), currentDto.getDescription(), currentDto.getCategory().name(), mainBytes, mainName, mainMime);

            String ekGoruntusuNotu = !uploadComponent.getUploadedFileNames().isEmpty() ? " [Ekler: " + String.join(", ", uploadComponent.getUploadedFileNames()) + "]" : "";
            systemLogService.log("Müşteri (" + loggedInEmail + ") yeni talep oluşturdu: " + currentDto.getTitle() + " [Kategori: " + currentDto.getCategory().getLabel() + "]" + ekGoruntusuNotu);
            notificationService.notifyRole("PO", "Yeni Talep Geldi", "İncelenmesi gereken yeni bir talep eklendi: " + currentDto.getTitle());

            Notification success = Notification.show(getTranslation("request.create.notif.success"), 3000, Notification.Position.TOP_CENTER);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            resetForm();

        } catch (Exception e) {
            Notification error = Notification.show(getTranslation("request.create.notif.error") + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}