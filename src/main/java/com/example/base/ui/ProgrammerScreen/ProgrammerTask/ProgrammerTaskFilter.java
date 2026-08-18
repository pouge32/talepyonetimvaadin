package com.example.base.ui.ProgrammerScreen.ProgrammerTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.WorkflowEntity;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class ProgrammerTaskFilter {

    private GridListDataView<WorkflowEntity> dataView;
    private String searchTerm = "";
    private Integer minScoreFilter = null;
    private LocalDateTime startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
    private LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
    private final Component context;

    public ProgrammerTaskFilter(Component context) {
        this.context = context;
    }

    public void setDataView(GridListDataView<WorkflowEntity> dataView) {
        this.dataView = dataView;
        this.dataView.addFilter(this::test);
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
        if (dataView != null) dataView.refreshAll();
    }

    public void setMinScoreFilter(Integer minScoreFilter) {
        this.minScoreFilter = minScoreFilter;
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

    public Component createStatusFilterHeader(Consumer<Integer> filterChangeConsumer) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setItems(999, 20, 10, 5);
        comboBox.setItemLabelGenerator(score -> {
            if (score >= 999) return context.getTranslation("programmer.combobox.Priority.urgent");
            if (score >= 20) return context.getTranslation("programmer.combobox.Priority.critical");
            if (score >= 10) return context.getTranslation("programmer.combobox.Priority.high");
            return context.getTranslation("programmer.combobox.Priority.normal");
        });
        comboBox.setPlaceholder("Öncelik");
        comboBox.setClearButtonVisible(true);
        comboBox.setWidth("110px");
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return comboBox;
    }

    public Component createDateRangeFilterHeader() {
        DatePicker startPicker = new DatePicker();
        startPicker.setPlaceholder("Başlangıç");
        startPicker.setClearButtonVisible(true);
        startPicker.setWidth("90px");
        startPicker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        startPicker.setValue(LocalDate.now().minusWeeks(1));
        setStartDate(LocalDate.now().minusWeeks(1).atStartOfDay());

        DatePicker endPicker = new DatePicker();
        endPicker.setPlaceholder("Bitiş");
        endPicker.setClearButtonVisible(true);
        endPicker.setWidth("90px");
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

        HorizontalLayout layout = new HorizontalLayout(startPicker, endPicker);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.addClassName("programmer-date-layout");
        return layout;
    }

    private boolean test(WorkflowEntity workflow) {
        boolean matchesSearch = true;
        if (!searchTerm.isEmpty()) {
            RequestEntity request = workflow.getRequest();
            if (request == null) return false;
            boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
            boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
            boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
            matchesSearch = inTitle || inDesc || inId;
        }

        boolean matchesStatus = true;
        if (minScoreFilter != null) {
            if (workflow.getRequest() == null || workflow.getRequest().getPrioritization() == null) {
                matchesStatus = false;
            } else {
                int score = workflow.getRequest().getPrioritization().getPriorityScore();
                if (minScoreFilter == 999) matchesStatus = (score >= 999);
                else if (minScoreFilter == 20) matchesStatus = (score >= 20 && score < 999);
                else if (minScoreFilter == 10) matchesStatus = (score >= 10 && score < 20);
                else if (minScoreFilter == 5) matchesStatus = (score >= 5 && score < 10);
            }
        }

        boolean matchesDate = true;
        if (workflow.getAssignedAt() != null) {
            if (startDate != null && workflow.getAssignedAt().isBefore(startDate)) matchesDate = false;
            if (endDate != null && workflow.getAssignedAt().isAfter(endDate)) matchesDate = false;
        }

        return matchesSearch && matchesStatus && matchesDate;
    }
}