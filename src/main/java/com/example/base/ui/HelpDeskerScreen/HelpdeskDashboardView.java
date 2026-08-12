package com.example.base.ui.HelpDeskerScreen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.example.base.repository.RequestRepository;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
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

@Route(value = "destek-performans", layout = MainLayout.class)
@RolesAllowed({"HELPDESK", "GODPANEL"})
public class HelpdeskDashboardView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final VerticalLayout dashboardContainer = new VerticalLayout(); 
    
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker endDatePicker = new DatePicker();

    public HelpdeskDashboardView(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
        
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/apexcharts");

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H3 title = new H3(getTranslation("helpdesk.dashboard.headerTitle"));
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        startDatePicker.setLabel(getTranslation("helpdesk.dashboard.startDate"));
        endDatePicker.setLabel(getTranslation("helpdesk.dashboard.endDate"));

        HorizontalLayout filterLayout = new HorizontalLayout(startDatePicker, endDatePicker);
        filterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        dashboardContainer.setWidthFull();
        dashboardContainer.setPadding(false); 
        dashboardContainer.getStyle().set("gap", "20px");
        
        add(title, filterLayout, dashboardContainer);
        refreshDashboard();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("helpdesk.dashboard.pageTitle");
    }

    private void refreshDashboard() {
        dashboardContainer.removeAll();

        long closedCount = requestRepository.countByStatus("KAPATILDI");
        long forwardedCount = requestRepository.countByStatus("INCELEMEDE");

        String successColor = "#10B981"; 
        String primaryColor = "#3B82F6"; 

        HorizontalLayout birinciSatir = new HorizontalLayout();
        birinciSatir.setWidthFull();
        birinciSatir.getStyle().set("gap", "20px");

        HorizontalLayout closedCard = createStatCard(getTranslation("helpdesk.dashboard.stat.directClosed"), String.valueOf(closedCount), VaadinIcon.CHECK_CIRCLE.create(), successColor);
        closedCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout forwardedCard = createStatCard(getTranslation("helpdesk.dashboard.stat.forwardedPo"), String.valueOf(forwardedCount), VaadinIcon.ARROW_RIGHT.create(), primaryColor);
        forwardedCard.getElement().getStyle().set("flex", "1");

        VerticalLayout donutCard = createChartCard(getTranslation("helpdesk.dashboard.chart.performanceDist"), "gercek-apex-grafik", "200px");
        donutCard.getElement().getStyle().set("flex", "1.2");

        birinciSatir.add(closedCard, forwardedCard, donutCard);

        HorizontalLayout ikinciSatir = new HorizontalLayout();
        ikinciSatir.setWidthFull();
        ikinciSatir.getStyle().set("gap", "20px");

        VerticalLayout areaCard = createChartCard(getTranslation("helpdesk.dashboard.chart.weeklyTrend"), "area-chart-div", "250px");
        areaCard.getElement().getStyle().set("flex", "1.5"); 

        VerticalLayout barCard = createChartCard(getTranslation("helpdesk.dashboard.chart.categoryDensity"), "bar-chart-div", "250px");
        barCard.getElement().getStyle().set("flex", "1");

        ikinciSatir.add(areaCard, barCard);
        
        HorizontalLayout ucuncuSatir = new HorizontalLayout();
        ucuncuSatir.setWidthFull();
        
        VerticalLayout frtCard = createChartCard(getTranslation("helpdesk.dashboard.chart.frt"), "frt-chart-div", "250px");
        frtCard.getElement().getStyle().set("flex", "1");
        ucuncuSatir.add(frtCard);

        dashboardContainer.add(birinciSatir, ikinciSatir, ucuncuSatir);

        List<String> barCategories = new ArrayList<>();
        List<Long> barData = new ArrayList<>();
        try {
            List<Object[]> categoryData = requestRepository.countRequestsByCategory();
            for (Object[] row : categoryData) {
                barCategories.add(row[0] != null ? row[0].toString() : getTranslation("helpdesk.dashboard.unknown"));
                barData.add(((Number) row[1]).longValue());
            }
        } catch (Exception e) {
            barCategories.addAll(List.of(getTranslation("helpdesk.dashboard.noData")));
            barData.add(0L);
        }

        List<String> trendGunleri = new ArrayList<>();
        List<Long> gelenTrend = new ArrayList<>();
        List<Long> cozulenTrend = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", UI.getCurrent().getLocale());
        LocalDate bugun = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate tarih = bugun.minusDays(i);
            trendGunleri.add(tarih.format(formatter)); 

            LocalDateTime gunBasi = tarih.atStartOfDay();
            LocalDateTime gunSonu = tarih.atTime(LocalTime.MAX);

            long oGunGelen = requestRepository.countByCreatedAtBetween(gunBasi, gunSonu);
            gelenTrend.add(oGunGelen);

            long oGunCozulen = requestRepository.countByStatusAndCreatedAtBetween("KAPATILDI", gunBasi, gunSonu);
            cozulenTrend.add(oGunCozulen);
        }

        double donutClosed = (closedCount == 0 && forwardedCount == 0) ? 0.1 : (double) closedCount;
        double donutForwarded = (closedCount == 0 && forwardedCount == 0) ? 0.1 : (double) forwardedCount;
        String color1 = (closedCount == 0 && forwardedCount == 0) ? "#E2E8F0" : successColor;
        String color2 = (closedCount == 0 && forwardedCount == 0) ? "#E2E8F0" : primaryColor;

        List<Integer> frtTrend = List.of(45, 38, 30, 25, 22, 18, 15);

        String labelClosed = getTranslation("helpdesk.dashboard.legend.closed");
        String labelForwarded = getTranslation("helpdesk.dashboard.legend.forwarded");
        String seriesIncoming = getTranslation("helpdesk.dashboard.series.incoming");
        String seriesResolved = getTranslation("helpdesk.dashboard.series.resolved");
        String seriesRequestCount = getTranslation("helpdesk.dashboard.series.requestCount");
        String seriesFrt = getTranslation("helpdesk.dashboard.series.frt");

        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  if (window.ApexCharts) {" +
            
            "    var donutEl = document.querySelector('#gercek-apex-grafik');" +
            "    if(donutEl) { donutEl.innerHTML = ''; new window.ApexCharts(donutEl, { series: [$0, $1], labels: [$4, $5], chart: { type: 'donut', height: 200, background: 'transparent', toolbar: { show: false } }, colors: [$2, $3], legend: { position: 'bottom' }, dataLabels: { enabled: false } }).render(); }" +

            "    var areaEl = document.querySelector('#area-chart-div');" +
            "    if(areaEl) { areaEl.innerHTML = ''; new window.ApexCharts(areaEl, { series: [{ name: $6, data: $8 }, { name: $7, data: $9 }], chart: { type: 'area', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#F59E0B', '#10B981'], dataLabels: { enabled: false }, stroke: { curve: 'smooth', width: 2 }, xaxis: { categories: $10 }, legend: { position: 'top' } }).render(); }" +

            "    var barEl = document.querySelector('#bar-chart-div');" +
            "    if(barEl) { barEl.innerHTML = ''; new window.ApexCharts(barEl, { series: [{ name: $11, data: $12 }], chart: { type: 'bar', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#3B82F6'], plotOptions: { bar: { borderRadius: 4, horizontal: true } }, dataLabels: { enabled: false }, xaxis: { categories: $13 } }).render(); }" +
            
            "    var frtEl = document.querySelector('#frt-chart-div');" +
            "    if(frtEl) { frtEl.innerHTML = ''; new window.ApexCharts(frtEl, { series: [{ name: $15, data: $14 }], chart: { type: 'line', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#F43F5E'], stroke: { curve: 'smooth', width: 3 }, markers: { size: 4 }, xaxis: { categories: $10 }, dataLabels: { enabled: true } }).render(); }" +
            
            "  }" +
            "}, 500);", 
            donutClosed, donutForwarded, color1, color2,
            labelClosed, labelForwarded, seriesIncoming, seriesResolved,
            gelenTrend, cozulenTrend, trendGunleri,
            seriesRequestCount, barData, barCategories,
            frtTrend, seriesFrt
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