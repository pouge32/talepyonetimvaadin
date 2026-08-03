package com.example.base.ui.HelpDeskerScreen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "destek-performans", layout = MainLayout.class)
@RolesAllowed("HELPDESK")
public class HelpdeskDashboardView extends VerticalLayout {

    private final RequestRepository requestRepository;
    private final VerticalLayout dashboardContainer = new VerticalLayout(); 
    
    private final DatePicker startDatePicker = new DatePicker("Başlangıç Tarihi");
    private final DatePicker endDatePicker = new DatePicker("Bitiş Tarihi");

    public HelpdeskDashboardView(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
        
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/apexcharts");

        H3 title = new H3("Destek Ekibi Performans Raporu");
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        HorizontalLayout filterLayout = new HorizontalLayout(startDatePicker, endDatePicker);
        filterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        dashboardContainer.setWidthFull();
        dashboardContainer.setPadding(false); 
        dashboardContainer.getStyle().set("gap", "20px");
        
        add(title, filterLayout, dashboardContainer);
        refreshDashboard();
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

        HorizontalLayout closedCard = createStatCard("Doğrudan Kapatılan", String.valueOf(closedCount), VaadinIcon.CHECK_CIRCLE.create(), successColor);
        closedCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout forwardedCard = createStatCard("PO'ya Sevk Edilen", String.valueOf(forwardedCount), VaadinIcon.ARROW_RIGHT.create(), primaryColor);
        forwardedCard.getElement().getStyle().set("flex", "1");

        VerticalLayout donutCard = createChartCard("Performans Dağılımı", "gercek-apex-grafik", "200px");
        donutCard.getElement().getStyle().set("flex", "1.2");

        birinciSatir.add(closedCard, forwardedCard, donutCard);

        HorizontalLayout ikinciSatir = new HorizontalLayout();
        ikinciSatir.setWidthFull();
        ikinciSatir.getStyle().set("gap", "20px");

        VerticalLayout areaCard = createChartCard("Haftalık Talep Trendi", "area-chart-div", "250px");
        areaCard.getElement().getStyle().set("flex", "1.5"); 

        VerticalLayout barCard = createChartCard("Kategori Bazlı Yoğunluk", "bar-chart-div", "250px");
        barCard.getElement().getStyle().set("flex", "1");

        ikinciSatir.add(areaCard, barCard);
        dashboardContainer.add(birinciSatir, ikinciSatir);

        List<String> barCategories = new ArrayList<>();
        List<Long> barData = new ArrayList<>();
        try {
            List<Object[]> categoryData = requestRepository.countRequestsByCategory();
            for (Object[] row : categoryData) {
                barCategories.add(row[0] != null ? row[0].toString() : "Belirsiz");
                barData.add(((Number) row[1]).longValue());
            }
        } catch (Exception e) {
            barCategories.addAll(List.of("Veri Yok"));
            barData.addAll(List.of(0L));
        }

        List<String> trendGunleri = new ArrayList<>();
        List<Long> gelenTrend = new ArrayList<>();
        List<Long> cozulenTrend = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("tr"));
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

        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  if (window.ApexCharts) {" +
            "    var donutEl = document.querySelector('#gercek-apex-grafik');" +
            "    if(donutEl) { donutEl.innerHTML = ''; new window.ApexCharts(donutEl, { series: [$0, $1], labels: ['Kapatılan', 'Sevk Edilen'], chart: { type: 'donut', height: 200, background: 'transparent', toolbar: { show: false } }, colors: [$2, $3], legend: { position: 'bottom' }, dataLabels: { enabled: false } }).render(); }" +

            "    var areaEl = document.querySelector('#area-chart-div');" +
            "    if(areaEl) { areaEl.innerHTML = ''; new window.ApexCharts(areaEl, { series: [{ name: 'Gelen Talepler', data: $4 }, { name: 'Çözülen Talepler', data: $5 }], chart: { type: 'area', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#F59E0B', '#10B981'], dataLabels: { enabled: false }, stroke: { curve: 'smooth', width: 2 }, xaxis: { categories: $6 }, legend: { position: 'top' } }).render(); }" +

            "    var barEl = document.querySelector('#bar-chart-div');" +
            "    if(barEl) { barEl.innerHTML = ''; new window.ApexCharts(barEl, { series: [{ name: 'Talep Sayısı', data: $7 }], chart: { type: 'bar', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#3B82F6'], plotOptions: { bar: { borderRadius: 4, horizontal: true } }, dataLabels: { enabled: false }, xaxis: { categories: $8 } }).render(); }" +
            "  }" +
            "}, 500);", 
            donutClosed, donutForwarded, color1, color2,
            gelenTrend, cozulenTrend, trendGunleri,
            barData, barCategories
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