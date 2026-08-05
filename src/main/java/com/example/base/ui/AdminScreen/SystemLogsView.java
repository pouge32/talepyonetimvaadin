package com.example.base.ui.AdminScreen;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.base.entity.SystemLogEntity;
import com.example.base.entity.UserEntity;
import com.example.base.repository.SystemLogRepository;
import com.example.base.repository.UserRepository;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/loglar", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SystemLogsView extends VerticalLayout implements HasDynamicTitle {

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    
    private final Grid<SystemLogEntity> grid = new Grid<>(SystemLogEntity.class, false);
    
    private int currentPage = 0;
    private final int PAGE_SIZE = 20;
    
    private String searchTerm = "";
    private String userEmailFilter = "";
    private LocalDateTime startDateFilter = null;
    private LocalDateTime endDateFilter = null;

    private final Button prevButton = new Button();
    private final Button nextButton = new Button();
    private final Span pageInfo = new Span();

    public SystemLogsView(SystemLogRepository systemLogRepository, UserRepository userRepository) { 
        this.systemLogRepository = systemLogRepository;
        this.userRepository = userRepository;

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
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 100px)")
                .set("display", "flex")
                .set("flex-direction", "column");

        H2 title = new H2(getTranslation("admin.logs.headerTitle"));
        title.getStyle().set("margin-top", "0");
        Paragraph subtitle = new Paragraph(getTranslation("admin.logs.headerSubtitle"));
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        TextField searchField = new TextField(getTranslation("admin.logs.searchLabel"));
        searchField.setPlaceholder(getTranslation("admin.logs.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");
        searchField.addValueChangeListener(e -> {
            this.searchTerm = e.getValue();
            this.currentPage = 0; 
            refreshGrid();
        });

        ComboBox<UserEntity> userFilterCombo = new ComboBox<>(getTranslation("admin.logs.userFilterLabel"));
        userFilterCombo.setItems(userRepository.findAll());
        userFilterCombo.setItemLabelGenerator(user -> user.getNameSurname() + " (" + user.getEmail() + ")");
        userFilterCombo.setPlaceholder(getTranslation("admin.logs.userFilterPlaceholder"));
        userFilterCombo.setClearButtonVisible(true);
        userFilterCombo.setWidth("280px");
        userFilterCombo.addValueChangeListener(e -> {
            this.userEmailFilter = e.getValue() != null ? e.getValue().getEmail() : "";
            this.currentPage = 0;
            refreshGrid();
        });

        DatePicker startDatePicker = new DatePicker(getTranslation("admin.logs.startDateLabel"));
        startDatePicker.setClearButtonVisible(true);
        startDatePicker.addValueChangeListener(e -> {
            this.startDateFilter = e.getValue() != null ? e.getValue().atStartOfDay() : null;
            this.currentPage = 0;
            refreshGrid();
        });

        DatePicker endDatePicker = new DatePicker(getTranslation("admin.logs.endDateLabel"));
        endDatePicker.setClearButtonVisible(true);
        endDatePicker.addValueChangeListener(e -> {
            this.endDateFilter = e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null;
            this.currentPage = 0;
            refreshGrid();
        });

        Button refreshButton = new Button(getTranslation("admin.logs.refreshBtn"), VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addClickListener(e -> {
            this.currentPage = 0;
            refreshGrid();
        });

        HorizontalLayout filterLayout = new HorizontalLayout(searchField, userFilterCombo, startDatePicker, endDatePicker, refreshButton);
        filterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        filterLayout.setWidthFull();
        filterLayout.getStyle().set("flex-wrap", "wrap").set("margin-bottom", "16px");

        configureGrid();
        
        mainContainer.add(title, subtitle, filterLayout, grid, buildPaginationBar());
        add(mainContainer);
        
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("admin.logs.pageTitle");
    }

    private void configureGrid() {
        grid.addColumn(SystemLogEntity::getLogId).setHeader(getTranslation("admin.logs.grid.id")).setAutoWidth(true).setFlexGrow(0); 
        grid.addColumn(SystemLogEntity::getAction).setHeader(getTranslation("admin.logs.grid.action")).setFlexGrow(1);
        
        grid.addColumn(log -> log.getCreatedAt() != null ? log.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) : "-")
            .setHeader(getTranslation("admin.logs.grid.date")).setAutoWidth(true).setFlexGrow(0);

        grid.setWidthFull();
        grid.getStyle().set("flex-grow", "1");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
    }

    private HorizontalLayout buildPaginationBar() {
        HorizontalLayout paginationBar = new HorizontalLayout();
        paginationBar.setWidthFull();
        paginationBar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        paginationBar.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationBar.getStyle().set("padding-top", "15px");

        prevButton.setText(getTranslation("admin.logs.pagination.prev"));
        prevButton.setIcon(VaadinIcon.ANGLE_LEFT.create());
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        nextButton.setText(getTranslation("admin.logs.pagination.next"));
        nextButton.setIcon(VaadinIcon.ANGLE_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.setIconAfterText(true);

        pageInfo.getStyle().set("font-weight", "bold").set("margin", "0 20px");

        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshGrid();
            }
        });

        nextButton.addClickListener(e -> {
            currentPage++;
            refreshGrid();
        });

        paginationBar.add(prevButton, pageInfo, nextButton);
        return paginationBar;
    }

    private void refreshGrid() {
        Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<SystemLogEntity> logPage = systemLogRepository.findFilteredLogs(
                searchTerm, userEmailFilter, startDateFilter, endDateFilter, pageable
        );
        
        grid.setItems(logPage.getContent());
        
        int totalPages = logPage.getTotalPages();
        if (totalPages == 0) totalPages = 1; 
        
        pageInfo.setText(getTranslation("admin.logs.pagination.info", (currentPage + 1), totalPages));
        
        prevButton.setEnabled(logPage.hasPrevious());
        nextButton.setEnabled(logPage.hasNext());
    }
}