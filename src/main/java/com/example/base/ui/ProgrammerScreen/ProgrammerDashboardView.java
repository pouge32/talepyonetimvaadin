package com.example.base.ui.ProgrammerScreen;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.WorkflowRepository;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "programmer-dashboard", layout = MainLayout.class)
@RolesAllowed({"PROGRAMMER","GODPANEL"})
public class ProgrammerDashboardView extends VerticalLayout implements HasDynamicTitle {

    private final WorkflowRepository workflowRepository;
    private final VerticalLayout dashboardContainer = new VerticalLayout(); 

    public ProgrammerDashboardView(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;

        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/apexcharts");

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H3 title = new H3(getTranslation("programmer.dashboard.headerTitle"));
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        dashboardContainer.setWidthFull();
        dashboardContainer.setPadding(false); 
        dashboardContainer.getStyle().set("gap", "20px");
        
        add(title, dashboardContainer);
        refreshDashboard();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("programmer.dashboard.pageTitle");
    }

    private void refreshDashboard() {
        dashboardContainer.removeAll();

        List<WorkflowEntity> allWorkflows = new ArrayList<>();
        try {
            allWorkflows = workflowRepository.findAll();
        } catch (Exception e) {}

        long backlogCount = allWorkflows.stream().filter(w -> "BACKLOG".equals(w.getWorkflowStatus())).count();
        long inDevCount = allWorkflows.stream().filter(w -> "IN DEVELOPMENT".equals(w.getWorkflowStatus())).count();
        long testCount = allWorkflows.stream().filter(w -> "TEST".equals(w.getWorkflowStatus())).count();
        long doneCount = allWorkflows.stream().filter(w -> "DONE".equals(w.getWorkflowStatus())).count();

        String backlogColor = "#64748B";
        String inDevColor = "#3B82F6";
        String testColor = "#F59E0B";
        String doneColor = "#10B981";

        HorizontalLayout statRow = new HorizontalLayout();
        statRow.setWidthFull();
        statRow.getStyle().set("gap", "20px");

        HorizontalLayout backlogCard = createStatCard("Backlog (Bekleyen)", String.valueOf(backlogCount), VaadinIcon.CLIPBOARD.create(), backlogColor);
        backlogCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout inDevCard = createStatCard("Geliştirmede (In Dev)", String.valueOf(inDevCount), VaadinIcon.CODE.create(), inDevColor);
        inDevCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout testCard = createStatCard("Test Aşamasında", String.valueOf(testCount), VaadinIcon.BUG.create(), testColor);
        testCard.getElement().getStyle().set("flex", "1");

        statRow.add(backlogCard, inDevCard, testCard);

        HorizontalLayout chartRow1 = new HorizontalLayout();
        chartRow1.setWidthFull();
        chartRow1.getStyle().set("gap", "20px");

        VerticalLayout statusCard = createChartCard(getTranslation("programmer.dashboard.chart.status"), "prog-status-chart", "280px");
        statusCard.getElement().getStyle().set("flex", "1");

        VerticalLayout activityCard = createChartCard(getTranslation("programmer.dashboard.chart.activity"), "prog-activity-chart", "280px");
        activityCard.getElement().getStyle().set("flex", "2"); 

        chartRow1.add(statusCard, activityCard);

        HorizontalLayout chartRow2 = new HorizontalLayout();
        chartRow2.setWidthFull();
        
        VerticalLayout performanceCard = createChartCard(getTranslation("programmer.dashboard.chart.performance"), "prog-perf-chart", "280px");
        performanceCard.getElement().getStyle().set("flex", "1");
        
        chartRow2.add(performanceCard);

        dashboardContainer.add(statRow, chartRow1, chartRow2);

        double dBacklog = backlogCount == 0 ? 0.1 : backlogCount;
        double dInDev = inDevCount == 0 ? 0.1 : inDevCount;
        double dTest = testCount == 0 ? 0.1 : testCount;
        double dDone = doneCount == 0 ? 0.1 : doneCount;

        List<String> trendGunleri = new ArrayList<>();
        List<Long> tamamlananTrend = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", UI.getCurrent().getLocale());
        LocalDate bugun = LocalDate.now();

        List<Long> mockTrendData = List.of(2L, 5L, 3L, 8L, 4L, 7L, doneCount);

        for (int i = 6; i >= 0; i--) {
            trendGunleri.add(bugun.minusDays(i).format(formatter)); 
        }
        tamamlananTrend.addAll(mockTrendData);

        List<String> perfUsers = List.of("100", "250", "500", "1000", "2500", "5000");
        List<Integer> perfLatency = List.of(45, 58, 115, 230, 850, 1950);

        String lblBacklog = "Backlog";
        String lblInDev = "In Development";
        String lblTest = "Test";
        String lblDone = "Done";
        String seriesCompleted = getTranslation("programmer.dashboard.series.completed");
        String seriesLatency = getTranslation("programmer.dashboard.series.latency");

        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  if (window.ApexCharts) {" +
            
            "    /* 1. Görev Durum Dağılımı (Donut) */" +
            "    var statusEl = document.querySelector('#prog-status-chart');" +
            "    if(statusEl) { statusEl.innerHTML = ''; new window.ApexCharts(statusEl, { series: [$0, $1, $2, $3], labels: [$8, $9, $10, $11], chart: { type: 'donut', height: 280, background: 'transparent', toolbar: { show: false } }, colors: [$4, $5, $6, $7], legend: { position: 'bottom' }, dataLabels: { enabled: false } }).render(); }" +

            "    /* 2. Geliştirici Aktivite Trendi (Area) */" +
            "    var actEl = document.querySelector('#prog-activity-chart');" +
            "    if(actEl) { actEl.innerHTML = ''; new window.ApexCharts(actEl, { series: [{ name: $12, data: $13 }], chart: { type: 'area', height: 280, background: 'transparent', toolbar: { show: false } }, colors: ['#10B981'], dataLabels: { enabled: false }, stroke: { curve: 'smooth', width: 2 }, xaxis: { categories: $14 }, legend: { position: 'top' } }).render(); }" +
            
            "    /* 3. Sistem Yük ve Performans Analizi (Line) - Geliştiriciler İçin */" +
            "    var perfEl = document.querySelector('#prog-perf-chart');" +
            "    if(perfEl) { perfEl.innerHTML = ''; new window.ApexCharts(perfEl, { series: [{ name: $17, data: $16 }], chart: { type: 'line', height: 280, background: 'transparent', toolbar: { show: false } }, colors: ['#EF4444'], stroke: { curve: 'smooth', width: 3 }, markers: { size: 5, colors: ['#EF4444'], strokeColors: '#fff', strokeWidth: 2 }, xaxis: { categories: $15, title: { text: 'Eşzamanlı Aktif Kullanıcı (Concurrent Connections)', style: { fontWeight: 500 } } }, yaxis: { title: { text: 'API Yanıt Süresi (ms)', style: { fontWeight: 500 } } }, dataLabels: { enabled: true, background: { enabled: true, foreColor: '#fff', borderRadius: 2, padding: 4, opacity: 0.9, dropShadow: { enabled: false } } }, legend: { position: 'top' } }).render(); }" +
            
            "  }" +
            "}, 500);", 
            dBacklog, dInDev, dTest, dDone,
            backlogColor, inDevColor, testColor, doneColor,
            lblBacklog, lblInDev, lblTest, lblDone,
            seriesCompleted, tamamlananTrend, trendGunleri,
            perfUsers, perfLatency, seriesLatency
        );
    }

    private VerticalLayout createChartCard(String title, String divId, String height) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
            .set("background", "var(--lumo-base-color, #ffffff)")
            .set("border-radius", "12px")
            .set("padding", "20px")
            .set("box-shadow", "0 4px 10px rgba(0,0,0,0.05)")
            .set("border", "1px solid var(--lumo-contrast-10pct)");
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setSpacing(true);

        Span chartTitle = new Span(title);
        chartTitle.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
        
        Div chartDiv = new Div();
        chartDiv.setId(divId); 
        chartDiv.setWidth("100%");
        chartDiv.setHeight(height);
        chartDiv.getStyle().set("display", "flex").set("justify-content", "center");

        card.add(chartTitle, chartDiv);
        return card;
    }

    private HorizontalLayout createStatCard(String title, String value, Icon icon, String color) {
        HorizontalLayout card = new HorizontalLayout();
        card.getStyle()
            .set("background", "var(--lumo-base-color, #ffffff)")
            .set("border-radius", "12px")
            .set("padding", "20px")
            .set("box-shadow", "0 4px 10px rgba(0,0,0,0.05)")
            .set("border", "1px solid var(--lumo-contrast-10pct)");
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setSpacing(true);

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("width", "48px")
            .set("height", "48px")
            .set("border-radius", "50%")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center");

        icon.getStyle().set("color", color);
        icon.setSize("24px");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setSpacing(false);
        textLayout.setPadding(false);
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
        
        H4 valueSpan = new H4(value);
        valueSpan.getStyle().set("margin", "5px 0 0 0").set("font-size", "24px").set("color", "var(--lumo-header-text-color)");
        
        textLayout.add(titleSpan, valueSpan);
        card.add(iconWrapper, textLayout);
        return card;
    }
}