package com.example.base.ui.HelpDeskerScreen.OnInceleme;

import java.time.LocalDateTime;

import com.example.base.entity.RequestEntity;
import com.vaadin.flow.component.grid.dataview.GridListDataView;

public class OnIncelemeFilter {

    private GridListDataView<RequestEntity> dataView;
    private String searchTerm = "";
    private String status = "";
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private Integer assignedUserIdFilter = null;
    private boolean isGodPanel = false;

    public void setGodPanel(boolean isGodPanel) {
        this.isGodPanel = isGodPanel;
        if (dataView != null) dataView.refreshAll();
    }

    public void setDataView(GridListDataView<RequestEntity> dataView) {
        this.dataView = dataView;
        this.dataView.addFilter(this::test);
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm != null ? searchTerm.toLowerCase().trim() : "";
        if (dataView != null) dataView.refreshAll();
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "";
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

    public boolean test(RequestEntity request) {
        boolean matchesSearch = true;
        if (!searchTerm.isEmpty()) {
            boolean inTitle = request.getTitle() != null && request.getTitle().toLowerCase().contains(searchTerm);
            boolean inDesc = request.getDescription() != null && request.getDescription().toLowerCase().contains(searchTerm);
            boolean inId = String.valueOf(request.getRequestId()).contains(searchTerm);
            matchesSearch = inTitle || inDesc || inId;
        }

        boolean matchesStatus = status.isEmpty() ||
                (request.getStatus() != null && request.getStatus().equalsIgnoreCase(status));

        boolean matchesDate = true;
        if (request.getCreatedAt() != null) {
            if (startDate != null && request.getCreatedAt().isBefore(startDate)) matchesDate = false;
            if (endDate != null && request.getCreatedAt().isAfter(endDate)) matchesDate = false;
        }

        boolean matchesAssignedUser = true;
        if (assignedUserIdFilter != null) {
            boolean isAssignedToMe = request.getAssignedUser() != null && 
                                     request.getAssignedUser().getUserId().equals(assignedUserIdFilter);
            
            boolean isAssignedToHelpdesk = isGodPanel && request.getAssignedUser() != null && 
                                           "HELPDESK".equals(request.getAssignedUser().getRole().name());
            
            boolean isMyPoolTask = "NEW".equals(request.getStatus()) || "DESTEK_KONTROL".equals(request.getStatus());
                                           
            matchesAssignedUser = isAssignedToMe || isAssignedToHelpdesk || isMyPoolTask;
        }

        return matchesSearch && matchesStatus && matchesDate && matchesAssignedUser;
    }
}