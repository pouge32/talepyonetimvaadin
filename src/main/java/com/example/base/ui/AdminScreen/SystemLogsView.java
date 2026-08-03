package com.example.base.ui.AdminScreen;

import java.time.LocalDateTime;
import java.util.List;

import com.example.base.entity.SystemLogEntity;
import com.example.base.repository.SystemLogRepository; 
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/loglar", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SystemLogsView extends VerticalLayout {

    private final SystemLogRepository systemLogRepository; 
    private final Grid<SystemLogEntity> grid = new Grid<>(SystemLogEntity.class, false);
    private GridListDataView<SystemLogEntity> dataView; 
    private final LogFilter logFilter = new LogFilter();

    public SystemLogsView(SystemLogRepository systemLogRepository) { 
        this.systemLogRepository = systemLogRepository;

        H3 title = new H3("Sistem Logları ve İşlem Geçmişi");

        TextField searchField = new TextField();
        searchField.setPlaceholder("İşlem detaylarında ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");
        searchField.addValueChangeListener(e -> logFilter.setSearchTerm(e.getValue()));

        DatePicker startDatePicker = new DatePicker("Başlangıç Tarihi");
        startDatePicker.setClearButtonVisible(true);
        startDatePicker.addValueChangeListener(e -> {
            logFilter.setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });

        DatePicker endDatePicker = new DatePicker("Bitiş Tarihi");
        endDatePicker.setClearButtonVisible(true);
        endDatePicker.addValueChangeListener(e -> {
            logFilter.setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        Button refreshButton = new Button("Yenile", VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(e -> refreshGrid());

        HorizontalLayout filterLayout = new HorizontalLayout(searchField, startDatePicker, endDatePicker, refreshButton);
        filterLayout.setAlignItems(Alignment.END);
        filterLayout.setWidthFull();

        configureGrid();
        add(title, filterLayout, grid);
        refreshGrid();

        setSizeFull();
        setPadding(true);
    }

    private void configureGrid() {
        grid.addColumn(SystemLogEntity::getLogId).setHeader("ID").setAutoWidth(true); 
        grid.addColumn(SystemLogEntity::getAction).setHeader("Yapılan İşlem / Detay").setAutoWidth(true);
        grid.addColumn(SystemLogEntity::getCreatedAt).setHeader("İşlem Tarihi").setAutoWidth(true);

        grid.setWidthFull();
    }

    private void refreshGrid() {
        List<SystemLogEntity> allLogs = systemLogRepository.findAll();
        dataView = grid.setItems(allLogs);
        logFilter.setDataView(dataView);
    }

    private static class LogFilter {
        private GridListDataView<SystemLogEntity> dataView; 
        private String searchTerm = "";
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public void setDataView(GridListDataView<SystemLogEntity> dataView) { 
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
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

        public boolean test(SystemLogEntity log) { 
            boolean matchesSearch = true;
            if (!searchTerm.isEmpty()) {
                boolean matchesAction = log.getAction() != null && log.getAction().toLowerCase().contains(searchTerm);
                boolean matchesId = String.valueOf(log.getLogId()).contains(searchTerm);
                matchesSearch = matchesAction || matchesId;
            }

            boolean matchesDate = true;
            if (log.getCreatedAt() != null) {
                if (startDate != null && log.getCreatedAt().isBefore(startDate)) {
                    matchesDate = false;
                }
                if (endDate != null && log.getCreatedAt().isAfter(endDate)) {
                    matchesDate = false;
                }
            }

            return matchesSearch && matchesDate;
        }
    }
}