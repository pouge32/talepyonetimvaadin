package com.example.base.ui;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.example.base.entity.RequestEntity;
import com.example.base.service.RequestService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "taleplerim", layout = MainLayout.class)
@RolesAllowed(value = "CUSTOMER")
public class TaleplerimView extends VerticalLayout {

    private final RequestService requestService;
    private final Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);

    public TaleplerimView(RequestService requestService) {
        this.requestService = requestService;

        add(new H2("Taleplerim"));

        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.addColumn(RequestEntity::getRequestId).setHeader("ID").setAutoWidth(true);
        Grid.Column<RequestEntity> titleColumn = grid.addColumn(RequestEntity::getTitle).setHeader("Başlık");
        Grid.Column<RequestEntity> descColumn = grid.addColumn(RequestEntity::getDescription).setHeader("Detay");
        Grid.Column<RequestEntity> dateColumn = grid.addColumn(RequestEntity::getCreatedAt).setHeader("Oluşturulma Tarihi");
        Grid.Column<RequestEntity> statusColumn = grid.addComponentColumn(this::createStatusBadge).setHeader("Durum").setAutoWidth(true);

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
        comboBox.setItems("Yeni", "İncelemede", "İşleme Alındı");
        comboBox.setPlaceholder("Durum seç...");
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.setReadOnly(false);
        comboBox.getElement().setProperty("readonly", true);
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
        String status = request.getStatus();
        Badge badge = new Badge(statusLabel(status));

        switch (status) {
            case "NEW" -> badge.addThemeVariants(BadgeVariant.CONTRAST);
            case "İncelemede" -> badge.addThemeVariants(BadgeVariant.WARNING);
            case "İş Akışına Dönüştü" -> badge.addThemeVariants(BadgeVariant.SUCCESS);
            default -> badge.addThemeVariants(BadgeVariant.ERROR);
        }

        return badge;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "NEW" -> "Yeni";
            case "İncelemede" -> "İncelemede";
            case "İş Akışına Dönüştü" -> "İşleme Alındı";
            default -> status;
        };
    }
}