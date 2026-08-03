package com.example.base.ui.AdminScreen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.base.repository.RequestRepository;
import com.example.base.repository.UserRepository;
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

@Route(value = "admin-dashboard", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    
    private final VerticalLayout dashboardContainer = new VerticalLayout(); 

    public AdminDashboardView(RequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;

        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/apexcharts");

        H3 title = new H3("Sistem Yönetim Paneli (Admin)");
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        dashboardContainer.setWidthFull();
        dashboardContainer.setPadding(false); 
        dashboardContainer.getStyle().set("gap", "20px");
        
        add(title, dashboardContainer);
        refreshDashboard();
    }

    private void refreshDashboard() {
        dashboardContainer.removeAll();

        long totalUsers = userRepository.count();
        long totalRequests = requestRepository.count();
        long closedRequests = requestRepository.countByStatus("KAPATILDI"); 
        
        long activeRequests = 0;
        try {
            activeRequests = requestRepository.countActiveRequests();
        } catch (Exception e) {
            activeRequests = totalRequests - closedRequests; 
        }

        String usersColor = "#8B5CF6"; 
        String requestsColor = "#F59E0B"; 
        String successColor = "#10B981"; 

        HorizontalLayout statRow = new HorizontalLayout();
        statRow.setWidthFull();
        statRow.getStyle().set("gap", "20px");

        HorizontalLayout userCard = createStatCard("Toplam Kullanıcı", String.valueOf(totalUsers), VaadinIcon.USERS.create(), usersColor);
        userCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout totalReqCard = createStatCard("Sistemdeki Toplam Talep", String.valueOf(totalRequests), VaadinIcon.CLIPBOARD_TEXT.create(), requestsColor);
        totalReqCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout closedReqCard = createStatCard("Çözülen Toplam Talep", String.valueOf(closedRequests), VaadinIcon.CHECK_SQUARE_O.create(), successColor);
        closedReqCard.getElement().getStyle().set("flex", "1");

        statRow.add(userCard, totalReqCard, closedReqCard);

        HorizontalLayout chartRow = new HorizontalLayout();
        chartRow.setWidthFull();
        chartRow.getStyle().set("gap", "20px");

        VerticalLayout activityCard = createChartCard("Sistem Aktivite Trendi", "admin-activity-chart", "280px");
        activityCard.getElement().getStyle().set("flex", "2"); 

        VerticalLayout statusCard = createChartCard("Genel Talep Dağılımı", "admin-status-chart", "280px");
        statusCard.getElement().getStyle().set("flex", "1");

        chartRow.add(activityCard, statusCard);
        dashboardContainer.add(statRow, chartRow);

        double dataKapatilan = (closedRequests == 0 && activeRequests == 0) ? 0.1 : (double) closedRequests;
        double dataAcik = (closedRequests == 0 && activeRequests == 0) ? 0.1 : (double) activeRequests;
        String color1 = (closedRequests == 0 && activeRequests == 0) ? "#E2E8F0" : successColor;
        String color2 = (closedRequests == 0 && activeRequests == 0) ? "#E2E8F0" : requestsColor;

        List<String> trendGunleri = new ArrayList<>();
        List<Long> yeniKullaniciTrend = new ArrayList<>();
        List<Long> yeniTalepTrend = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("tr"));
        LocalDate bugun = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate tarih = bugun.minusDays(i);
            trendGunleri.add(tarih.format(formatter)); 

            LocalDateTime gunBasi = tarih.atStartOfDay();
            LocalDateTime gunSonu = tarih.atTime(LocalTime.MAX);

            long oGunGelenKullanici = 0;
            long oGunGelenTalep = 0;
            
            try {
                oGunGelenKullanici = userRepository.countByCreatedAtBetween(gunBasi, gunSonu);
                oGunGelenTalep = requestRepository.countByCreatedAtBetween(gunBasi, gunSonu);
            } catch (Exception e) {
            }

            yeniKullaniciTrend.add(oGunGelenKullanici);
            yeniTalepTrend.add(oGunGelenTalep);
        }

        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  if (window.ApexCharts) {" +
            
            "    var statusEl = document.querySelector('#admin-status-chart');" +
            "    if(statusEl) { statusEl.innerHTML = ''; new window.ApexCharts(statusEl, { series: [$0, $1], labels: ['Kapatılan', 'Açık/İşlemde'], chart: { type: 'donut', height: 250, background: 'transparent', toolbar: { show: false } }, colors: [$2, $3], legend: { position: 'bottom' }, dataLabels: { enabled: false } }).render(); }" +

            "    var actEl = document.querySelector('#admin-activity-chart');" +
            "    if(actEl) { actEl.innerHTML = ''; new window.ApexCharts(actEl, { series: [{ name: 'Yeni Kullanıcılar', data: $4 }, { name: 'Yeni Talepler', data: $5 }], chart: { type: 'area', height: 280, background: 'transparent', toolbar: { show: false } }, colors: ['#8B5CF6', '#3B82F6'], dataLabels: { enabled: false }, stroke: { curve: 'smooth', width: 2 }, xaxis: { categories: $6 }, legend: { position: 'top' } }).render(); }" +
            
            "  }" +
            "}, 500);", 
            dataKapatilan, dataAcik, color1, color2,
            yeniKullaniciTrend, yeniTalepTrend, trendGunleri
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