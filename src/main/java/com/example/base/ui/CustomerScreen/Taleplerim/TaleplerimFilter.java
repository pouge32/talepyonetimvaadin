package com.example.base.ui.CustomerScreen.Taleplerim;

import java.time.LocalDateTime;

import com.example.base.entity.RequestEntity;
import com.vaadin.flow.component.grid.dataview.GridListDataView;

public class TaleplerimFilter {
    private final GridListDataView<RequestEntity> dataView;

    private String searchTerm = "";
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status = "";

    public TaleplerimFilter(GridListDataView<RequestEntity> dataView) {
        this.dataView = dataView;
        this.dataView.addFilter(this::test);
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
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
        boolean matchesSearch = true;
        if (!searchTerm.isEmpty()) {
            boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
            boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
            boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
            matchesSearch = inTitle || inDesc || inId;
        }

        boolean matchesDate = true;
        if (request.getCreatedAt() != null) {
            if (startDate != null && request.getCreatedAt().isBefore(startDate)) {
                matchesDate = false;
            }
            if (endDate != null && request.getCreatedAt().isAfter(endDate)) {
                matchesDate = false;
            }
        }
        return matchesSearch && matchesDate;
    }
}