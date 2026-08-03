package com.example.base.ui.PoScreen;

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

@Route(value = "po-dashboard", layout = MainLayout.class)
@RolesAllowed("PO")
public class PODashboardView extends VerticalLayout {

    private final RequestRepository requestRepository;
    private final VerticalLayout dashboardContainer = new VerticalLayout(); 

    public PODashboardView(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;

        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/apexcharts");

        H3 title = new H3("Proje Sorumlusu (PO) İş Yükü Paneli");
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        dashboardContainer.setWidthFull();
        dashboardContainer.setPadding(false); 
        dashboardContainer.getStyle().set("gap", "20px");
        
        add(title, dashboardContainer);
        refreshDashboard();
    }

    private void refreshDashboard() {
        dashboardContainer.removeAll();

        long bekleyenGorevler = 0;
        long tamamlananGorevler = 0; 
        long bugunGelenGorevler = 0;
        
        LocalDateTime gunBasi = LocalDate.now().atStartOfDay();
        LocalDateTime gunSonu = LocalDate.now().atTime(LocalTime.MAX);
        
        try {
            bekleyenGorevler = requestRepository.countByStatus("INCELEMEDE");
            tamamlananGorevler = requestRepository.countByStatus("KAPATILDI"); 
            bugunGelenGorevler = requestRepository.countByStatusAndCreatedAtBetween("INCELEMEDE", gunBasi, gunSonu);
        } catch (Exception e) {
        }

        String warningColor = "#F59E0B"; 
        String successColor = "#10B981"; 
        String infoColor = "#3B82F6";  

        HorizontalLayout statRow = new HorizontalLayout();
        statRow.setWidthFull();
        statRow.getStyle().set("gap", "20px");

        HorizontalLayout pendingCard = createStatCard("Bekleyen (Sevk Edilen)", String.valueOf(bekleyenGorevler), VaadinIcon.CLOCK.create(), warningColor);
        pendingCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout todayCard = createStatCard("Bugün Atananlar", String.valueOf(bugunGelenGorevler), VaadinIcon.CALENDAR_CLOCK.create(), infoColor);
        todayCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout completedCard = createStatCard("Çözüme Kavuşturulan", String.valueOf(tamamlananGorevler), VaadinIcon.CHECK_SQUARE_O.create(), successColor);
        completedCard.getElement().getStyle().set("flex", "1");

        statRow.add(pendingCard, todayCard, completedCard);

        HorizontalLayout chartRow1 = new HorizontalLayout();
        chartRow1.setWidthFull();
        
        VerticalLayout activityCard = createChartCard("Haftalık Çözüm Trendi", "po-activity-chart", "250px");
        activityCard.getElement().getStyle().set("flex", "1"); 
        chartRow1.add(activityCard);

        HorizontalLayout chartRow2 = new HorizontalLayout();
        chartRow2.setWidthFull();
        chartRow2.getStyle().set("gap", "20px");

        VerticalLayout statusCard = createChartCard("Mevcut İş Yükü Dağılımı", "po-status-chart", "250px");
        statusCard.getElement().getStyle().set("flex", "1");

        VerticalLayout categoryCard = createChartCard("Kategoriye Göre Bekleyen İşler", "po-category-chart", "250px");
        categoryCard.getElement().getStyle().set("flex", "1.5");

        chartRow2.add(statusCard, categoryCard);

        dashboardContainer.add(statRow, chartRow1, chartRow2);

        double dataBekleyen = (bekleyenGorevler == 0 && tamamlananGorevler == 0) ? 0.1 : (double) bekleyenGorevler;
        double dataTamamlanan = (bekleyenGorevler == 0 && tamamlananGorevler == 0) ? 0.1 : (double) tamamlananGorevler;
        String color1 = (bekleyenGorevler == 0 && tamamlananGorevler == 0) ? "#E2E8F0" : warningColor;
        String color2 = (bekleyenGorevler == 0 && tamamlananGorevler == 0) ? "#E2E8F0" : successColor;

        List<String> barCategories = new ArrayList<>();
        List<Long> barData = new ArrayList<>();
        try {
            List<Object[]> categoryData = requestRepository.countPendingRequestsByCategory();
            for (Object[] row : categoryData) {
                barCategories.add(row[0] != null ? row[0].toString() : "Belirsiz");
                barData.add(((Number) row[1]).longValue());
            }
        } catch (Exception e) {
            barCategories.add("Veri Yok");
            barData.add(0L);
        }

        List<String> trendGunleri = new ArrayList<>();
        List<Long> gelenGorevTrend = new ArrayList<>();
        List<Long> cozumTrend = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("tr"));
        LocalDate bugun = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate tarih = bugun.minusDays(i);
            trendGunleri.add(tarih.format(formatter)); 

            LocalDateTime loopGunBasi = tarih.atStartOfDay();
            LocalDateTime loopGunSonu = tarih.atTime(LocalTime.MAX);

            long oGunSevkEdilen = 0;
            long oGunCozulen = 0;
            
            try {
                oGunSevkEdilen = requestRepository.countByStatusAndCreatedAtBetween("INCELEMEDE", loopGunBasi, loopGunSonu);
                oGunCozulen = requestRepository.countByStatusAndCreatedAtBetween("KAPATILDI", loopGunBasi, loopGunSonu);
            } catch (Exception e) { }

            gelenGorevTrend.add(oGunSevkEdilen);
            cozumTrend.add(oGunCozulen);
        }

        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  if (window.ApexCharts) {" +
            
            "    /* Donut Chart */" +
            "    var statusEl = document.querySelector('#po-status-chart');" +
            "    if(statusEl) { statusEl.innerHTML = ''; new window.ApexCharts(statusEl, { series: [$0, $1], labels: ['İncelemede', 'Çözülen'], chart: { type: 'donut', height: 250, background: 'transparent', toolbar: { show: false } }, colors: [$2, $3], legend: { position: 'bottom' }, dataLabels: { enabled: false } }).render(); }" +

            "    /* Area Chart */" +
            "    var actEl = document.querySelector('#po-activity-chart');" +
            "    if(actEl) { actEl.innerHTML = ''; new window.ApexCharts(actEl, { series: [{ name: 'Bana Atananlar', data: $4 }, { name: 'Çözdüklerim', data: $5 }], chart: { type: 'area', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#F59E0B', '#10B981'], dataLabels: { enabled: false }, stroke: { curve: 'smooth', width: 2 }, xaxis: { categories: $6 }, legend: { position: 'top' } }).render(); }" +
            
            "    /* Bar Chart */" +
            "    var barEl = document.querySelector('#po-category-chart');" +
            "    if(barEl) { barEl.innerHTML = ''; new window.ApexCharts(barEl, { series: [{ name: 'Bekleyen İş', data: $7 }], chart: { type: 'bar', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#EF4444'], plotOptions: { bar: { borderRadius: 4, horizontal: true } }, dataLabels: { enabled: false }, xaxis: { categories: $8 } }).render(); }" +

            "  }" +
            "}, 500);", 
            dataBekleyen, dataTamamlanan, color1, color2,
            gelenGorevTrend, cozumTrend, trendGunleri,
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