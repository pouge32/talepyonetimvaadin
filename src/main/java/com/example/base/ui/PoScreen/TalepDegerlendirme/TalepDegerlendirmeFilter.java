package com.example.base.ui.PoScreen.TalepDegerlendirme;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.base.entity.RequestEntity;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class TalepDegerlendirmeFilter {

    private GridListDataView<RequestEntity> dataView;
    private String assignee = "";
    private String searchTerm = "";
    private LocalDateTime startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
    private LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
    private Integer assignedUserIdFilter = null;
    private boolean isGodPanel = false;
    private final Component context;

    public TalepDegerlendirmeFilter(Component context) {
        this.context = context;
    }

    public void setGodPanel(boolean isGodPanel) {
        this.isGodPanel = isGodPanel;
        if (dataView != null) dataView.refreshAll();
    }

    public void setDataView(GridListDataView<RequestEntity> dataView) {
        this.dataView = dataView;
        this.dataView.addFilter(this::test);
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee != null ? assignee : "";
        if (dataView != null) dataView.refreshAll();
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

    public void setAssignedUserIdFilter(Integer assignedUserIdFilter) {
        this.assignedUserIdFilter = assignedUserIdFilter;
        if (dataView != null) dataView.refreshAll();
    }

    public Component createComboBoxFilterHeader(String placeholder) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(
                context.getTranslation("po.eval.team.support"),
                context.getTranslation("po.eval.team.po"),
                context.getTranslation("po.eval.team.software"),
                context.getTranslation("requests.status.closed")
        );
        comboBox.setPlaceholder(placeholder);
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.addValueChangeListener(e -> setAssignee(e.getValue()));
        return comboBox;
    }

    public Component createDateRangeFilterHeader() {
        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder("İlk");
        startPicker.setClearButtonVisible(true);
        startPicker.setWidth("85px");
        startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        startPicker.setValue(LocalDate.now().minusWeeks(1));
        setStartDate(LocalDate.now().minusWeeks(1).atStartOfDay());

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder("Son");
        endPicker.setClearButtonVisible(true);
        endPicker.setWidth("85px");
        endPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        endPicker.setValue(LocalDate.now());
        setEndDate(LocalDate.now().atTime(23, 59, 59));

        startPicker.addValueChangeListener(e -> {
            endPicker.setMin(e.getValue());
            setStartDate(e.getValue() != null ? e.getValue().atStartOfDay() : null);
        });

        endPicker.addValueChangeListener(e -> {
            startPicker.setMax(e.getValue());
            setEndDate(e.getValue() != null ? e.getValue().atTime(23, 59, 59) : null);
        });

        HorizontalLayout dateLayout = new HorizontalLayout(startPicker, endPicker);
        dateLayout.setPadding(false);
        dateLayout.setSpacing(false);
        dateLayout.addClassName("po-eval-date-layout");
        return dateLayout;
    }

    private boolean test(RequestEntity request) {
        String mappedAssignee = request.getStatus();
        if ("NEW".equals(request.getStatus()) || "DESTEK_KONTROL".equals(request.getStatus())) mappedAssignee = "Destek Ekibi";
        else if ("INCELEMEDE".equals(request.getStatus()) || "PO_KONTROL".equals(request.getStatus())) mappedAssignee = "Ürün Yönetimi";
        else if ("ONAYLANDI".equals(request.getStatus()) || "İş Akışına Dönüştü".equals(request.getStatus())) mappedAssignee = "Yazılım Ekibi";

        boolean matchesAssignee = matches(mappedAssignee, assignee);

        boolean matchesSearch = true;
        if (!searchTerm.isEmpty()) {
            boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
            boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
            boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
            matchesSearch = inTitle || inDesc || inId;
        }

        boolean matchesDate = true;
        if (request.getCreatedAt() != null) {
            if (startDate != null && request.getCreatedAt().isBefore(startDate)) matchesDate = false;
            if (endDate != null && request.getCreatedAt().isAfter(endDate)) matchesDate = false;
        }

        boolean matchesAssignedUser = true;
        if (assignedUserIdFilter != null) {
            boolean isAssignedToMe = request.getAssignedUser() != null && request.getAssignedUser().getUserId().equals(assignedUserIdFilter);
            boolean isAssignedToPO = isGodPanel && request.getAssignedUser() != null && "PO".equals(request.getAssignedUser().getRole().name());
            boolean isInReviewPool = request.getAssignedUser() == null && ("INCELEMEDE".equals(request.getStatus()) || "PO_KONTROL".equals(request.getStatus()));

            matchesAssignedUser = isAssignedToMe || isAssignedToPO || isInReviewPool;
        }

        return matchesAssignee && matchesSearch && matchesDate && matchesAssignedUser;
    }

    private boolean matches(String value, String searchTerm) {
        return searchTerm == null || searchTerm.isEmpty() ||
                (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
    }
}