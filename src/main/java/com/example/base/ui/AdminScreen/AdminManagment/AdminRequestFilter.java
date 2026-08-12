package com.example.base.ui.AdminScreen.AdminManagment;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.base.entity.RequestEntity;
import com.vaadin.flow.component.grid.dataview.GridListDataView;

public class AdminRequestFilter {
    private GridListDataView<RequestEntity> dataView;
    private String title = "";
    private String description = "";
    private String status = "";
    private Integer minScoreFilter = null;
    private LocalDateTime startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
    private LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);

    public void setDataView(GridListDataView<RequestEntity> dataView) {
        this.dataView = dataView;
        this.dataView.addFilter(this::test);
    }

    public void setTitle(String t) { this.title = t; refresh(); }
    public void setDescription(String d) { this.description = d; refresh(); }
    public void setStatus(String s) { this.status = s; refresh(); }
    public void setMinScoreFilter(Integer score) { this.minScoreFilter = score; refresh(); }
    public void setStartDate(LocalDateTime s) { this.startDate = s; refresh(); }
    public void setEndDate(LocalDateTime e) { this.endDate = e; refresh(); }

    private void refresh() { 
        if (dataView != null) dataView.refreshAll(); 
    }

    public boolean test(RequestEntity request) {
        boolean mTitle = matches(request.getTitle(), title);
        boolean mDesc = matches(request.getDescription(), description);
        boolean mStatus = matches(request.getStatus(), status);
        boolean mDate = true;
        
        if (request.getCreatedAt() != null) {
            if (startDate != null && request.getCreatedAt().isBefore(startDate)) mDate = false;
            if (endDate != null && request.getCreatedAt().isAfter(endDate)) mDate = false;
        }

        boolean mPriority = true;
        if (minScoreFilter != null) {
            if (request.getPrioritization() == null) {
                mPriority = false;
            } else {
                int score = request.getPrioritization().getPriorityScore();
                if (minScoreFilter == 999) mPriority = (score >= 999);
                else if (minScoreFilter == 20) mPriority = (score >= 20 && score < 999);
                else if (minScoreFilter == 10) mPriority = (score >= 10 && score < 20);
                else if (minScoreFilter == 5) mPriority = (score >= 5 && score < 10);
                else if (minScoreFilter == 0) mPriority = (score < 5);
            }
        }
        return mTitle && mDesc && mStatus && mDate && mPriority;
    }

    private boolean matches(String val, String search) {
        if (search == null || search.isEmpty()) return true;
        return val != null && val.toLowerCase().contains(search.toLowerCase());
    }
}