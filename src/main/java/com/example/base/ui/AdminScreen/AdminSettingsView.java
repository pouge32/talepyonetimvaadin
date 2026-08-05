package com.example.base.ui.AdminScreen;

import java.util.List;

import com.example.base.entity.CategoryEntity;
import com.example.base.service.SettingsService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/ayarlar", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminSettingsView extends VerticalLayout implements HasDynamicTitle {

    private final SettingsService settingsService;

    private final Div categoryPanel = new Div();
    private final Div slaPanel = new Div();

    private final Grid<CategoryEntity> categoryGrid = new Grid<>(CategoryEntity.class, false);

    private final Tab tabCategories = new Tab();
    private final Tab tabSla = new Tab();
    private final Tabs tabs = new Tabs(tabCategories, tabSla);

    public AdminSettingsView(SettingsService settingsService) {
        this.settingsService = settingsService;

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
                .set("padding", "30px")
                .set("max-width", "1000px")
                .set("margin", "0 auto")
                .set("min-height", "600px");

        H2 headerTitle = new H2(getTranslation("admin.settings.headerTitle"));
        headerTitle.getStyle().set("margin-top", "0");
        Paragraph headerDesc = new Paragraph(getTranslation("admin.settings.headerDesc"));
        headerDesc.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-bottom", "20px");

        tabCategories.setLabel(getTranslation("admin.settings.tab.categories"));
        tabSla.setLabel(getTranslation("admin.settings.tab.sla"));

        tabs.setWidthFull();
        tabs.getStyle().set("margin-bottom", "20px");
        
        tabs.addSelectedChangeListener(e -> {
            categoryPanel.setVisible(tabs.getSelectedIndex() == 0);
            slaPanel.setVisible(tabs.getSelectedIndex() == 1);
        });

        categoryPanel.setWidthFull();
        slaPanel.setWidthFull();

        buildCategoryPanel();
        buildSlaPanel();
        
        slaPanel.setVisible(false);

        mainContainer.add(headerTitle, headerDesc, tabs, categoryPanel, slaPanel);
        add(mainContainer);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.settings.pageTitle");
    }

    private void buildCategoryPanel() {
        categoryPanel.removeAll();

        H3 title = new H3(getTranslation("admin.settings.category.title"));
        title.getStyle().set("margin-top", "0");

        TextField newCategoryField = new TextField();
        newCategoryField.setPlaceholder(getTranslation("admin.settings.category.placeholder"));
        newCategoryField.setWidthFull();

        Button addBtn = new Button(getTranslation("admin.settings.btn.add"), e -> {
            String value = newCategoryField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show(getTranslation("admin.settings.category.errorEmpty"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                settingsService.addCategory(value.trim());
                newCategoryField.clear();
                refreshCategoryGrid();
                Notification.show(getTranslation("admin.settings.category.successAdded"), 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout addRow = new HorizontalLayout(newCategoryField, addBtn);
        addRow.setWidthFull();
        addRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        addRow.getStyle().set("margin-bottom", "20px");

        categoryGrid.setWidthFull();
        categoryGrid.setHeight("400px");
        
        categoryGrid.addColumn(CategoryEntity::getName)
                .setHeader(getTranslation("admin.settings.grid.name"))
                .setFlexGrow(1);
                
        categoryGrid.addComponentColumn(this::createActiveToggle)
                .setHeader(getTranslation("admin.settings.grid.status"))
                .setAutoWidth(true).setFlexGrow(0);
                
        categoryGrid.addComponentColumn(this::createDeleteButton)
                .setHeader(getTranslation("admin.settings.grid.action"))
                .setAutoWidth(true).setFlexGrow(0);

        refreshCategoryGrid();

        categoryPanel.add(title, addRow, categoryGrid);
    }

    private Component createActiveToggle(CategoryEntity category) {
        Button toggleBtn = new Button(category.isActive() ? getTranslation("admin.settings.btn.makePassive") : getTranslation("admin.settings.btn.makeActive"));
        toggleBtn.addThemeVariants(category.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        toggleBtn.addClickListener(e -> {
            settingsService.setCategoryActive(category.getCategoryId(), !category.isActive());
            refreshCategoryGrid();
        });
        return toggleBtn;
    }

    private Component createDeleteButton(CategoryEntity category) {
        Button deleteBtn = new Button(getTranslation("admin.settings.btn.deleteAll"));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        deleteBtn.addClickListener(e -> {
            settingsService.deleteCategory(category.getCategoryId());
            refreshCategoryGrid();
            Notification.show(getTranslation("admin.settings.category.successDeleted"), 2000, Notification.Position.TOP_CENTER);
        });
        return deleteBtn;
    }

    private void refreshCategoryGrid() {
        List<CategoryEntity> all = settingsService.getAllCategories(); 
        categoryGrid.setItems(all);
    }

    private void buildSlaPanel() {
        slaPanel.removeAll();

        H3 title = new H3(getTranslation("admin.settings.sla.title"));
        title.getStyle().set("margin-top", "0");
        
        Paragraph info = new Paragraph(getTranslation("admin.settings.sla.info"));
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        NumberField limitField = new NumberField(getTranslation("admin.settings.sla.limitLabel"));
        limitField.setValue((double) settingsService.getSlaLimitHours());
        limitField.setMin(1);
        limitField.setStep(1);
        limitField.setWidthFull();

        NumberField warningField = new NumberField(getTranslation("admin.settings.sla.warningLabel"));
        warningField.setValue(settingsService.getSlaWarningPercent() * 100);
        warningField.setMin(1);
        warningField.setMax(100);
        warningField.setStep(5);
        warningField.setHelperText(getTranslation("admin.settings.sla.warningHelper"));
        warningField.setWidthFull();

        Button saveBtn = new Button(getTranslation("admin.settings.btn.saveSettings"), e -> {
            if (limitField.getValue() == null || warningField.getValue() == null) {
                Notification.show(getTranslation("admin.settings.sla.errorEmpty"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            long hours = limitField.getValue().longValue();
            double percent = warningField.getValue() / 100.0;
            settingsService.updateSlaSettings(hours, percent);
            Notification.show(getTranslation("admin.settings.sla.successSaved"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("margin-top", "20px");

        VerticalLayout form = new VerticalLayout(limitField, warningField, saveBtn);
        form.setPadding(false);
        form.setSpacing(false);
        form.setMaxWidth("400px");

        slaPanel.add(title, info, form);
    }
}