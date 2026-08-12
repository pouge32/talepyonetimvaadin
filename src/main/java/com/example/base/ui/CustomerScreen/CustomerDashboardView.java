package com.example.base.ui.CustomerScreen; 

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.UserEntity;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "musteri-dashboard", layout = MainLayout.class)
@RolesAllowed({"CUSTOMER", "GODPANEL"}) 
public class CustomerDashboardView extends VerticalLayout implements HasDynamicTitle {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository; 
    private final VerticalLayout dashboardContainer = new VerticalLayout(); 

    public CustomerDashboardView(RequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;

        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/apexcharts");

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        H3 title = new H3(getTranslation("customer.dashboard.headerTitle"));
        title.getStyle().set("margin-top", "0").set("color", "var(--lumo-header-text-color)");

        dashboardContainer.setWidthFull();
        dashboardContainer.setPadding(false); 
        dashboardContainer.getStyle().set("gap", "20px");
        
        add(title, dashboardContainer);
        refreshDashboard();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("customer.dashboard.pageTitle");
    }

    private void refreshDashboard() {
        dashboardContainer.removeAll();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication != null ? authentication.getName() : ""; 

        Integer currentCustomerId = -1; 
        
        try {
            UserEntity currentUser = userRepository.findByEmail(currentUsername).orElse(null); 
            if (currentUser != null) {
                currentCustomerId = currentUser.getUserId();
            }
        } catch (Exception e) {
            System.out.println("Kullanıcı bulunamadı.");
        }
        
        List<RequestEntity> myRequests = new ArrayList<>();
        try {
            if (currentCustomerId != -1) {
                myRequests = requestRepository.findByCustomer_UserId(currentCustomerId);
            }
        } catch (Exception e) {
        }

        long totalRequests = myRequests.size();
        long closedRequests = myRequests.stream().filter(r -> "KAPATILDI".equals(r.getStatus())).count();
        long pendingRequests = totalRequests - closedRequests; 
        long inReviewRequests = myRequests.stream().filter(r -> "INCELEMEDE".equals(r.getStatus())).count();

        String primaryColor = "#3B82F6"; 
        String warningColor = "#F59E0B"; 
        String successColor = "#10B981"; 

        HorizontalLayout statRow = new HorizontalLayout();
        statRow.setWidthFull();
        statRow.getStyle().set("gap", "20px");

        HorizontalLayout totalCard = createStatCard(getTranslation("customer.dashboard.stat.total"), String.valueOf(totalRequests), VaadinIcon.TICKET.create(), primaryColor);
        totalCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout reviewCard = createStatCard(getTranslation("customer.dashboard.stat.inReview"), String.valueOf(inReviewRequests), VaadinIcon.SEARCH.create(), warningColor);
        reviewCard.getElement().getStyle().set("flex", "1");

        HorizontalLayout closedCard = createStatCard(getTranslation("customer.dashboard.stat.resolved"), String.valueOf(closedRequests), VaadinIcon.CHECK_CIRCLE.create(), successColor);
        closedCard.getElement().getStyle().set("flex", "1");

        statRow.add(totalCard, reviewCard, closedCard);

        HorizontalLayout chartRow1 = new HorizontalLayout();
        chartRow1.setWidthFull();
        chartRow1.getStyle().set("gap", "20px");
        
        VerticalLayout activityCard = createChartCard(getTranslation("customer.dashboard.chart.activity"), "customer-activity-chart", "250px");
        activityCard.getElement().getStyle().set("flex", "1"); 

        VerticalLayout slaCard = createChartCard(getTranslation("customer.dashboard.chart.sla"), "customer-sla-chart", "250px");
        slaCard.getElement().getStyle().set("flex", "1");

        chartRow1.add(activityCard, slaCard);

        HorizontalLayout chartRow2 = new HorizontalLayout();
        chartRow2.setWidthFull();
        chartRow2.getStyle().set("gap", "20px");

        VerticalLayout statusCard = createChartCard(getTranslation("customer.dashboard.chart.status"), "customer-status-chart", "250px");
        statusCard.getElement().getStyle().set("flex", "1");

        VerticalLayout detailCard = createChartCard(getTranslation("customer.dashboard.chart.distribution"), "customer-detail-chart", "250px");
        detailCard.getElement().getStyle().set("flex", "1.2");

        chartRow2.add(statusCard, detailCard);

        dashboardContainer.add(statRow, chartRow1, chartRow2);

        double dataKapatilan = (totalRequests == 0) ? 0.1 : (double) closedRequests;
        double dataİslemde = (totalRequests == 0) ? 0.1 : (double) pendingRequests;
        String color1 = (totalRequests == 0) ? "#E2E8F0" : successColor;
        String color2 = (totalRequests == 0) ? "#E2E8F0" : warningColor;

        List<String> trendGunleri = new ArrayList<>();
        List<Long> acilanTalepTrend = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", UI.getCurrent().getLocale());
        LocalDate bugun = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate tarih = bugun.minusDays(i);
            trendGunleri.add(tarih.format(formatter)); 

            long oGunAcilan = myRequests.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().isEqual(tarih))
                .count();

            acilanTalepTrend.add(oGunAcilan);
        }

        List<Integer> slaTrend = List.of(12, 8, 5, 4, 3, 2, 2); 

        String labelResolved = getTranslation("customer.dashboard.legend.resolved");
        String labelInProgress = getTranslation("customer.dashboard.legend.inProgress");
        String seriesRequests = getTranslation("customer.dashboard.series.myRequests");
        String seriesCount = getTranslation("customer.dashboard.series.requestCount");
        String catResolved = getTranslation("customer.dashboard.category.resolved");
        String catPending = getTranslation("customer.dashboard.category.pending");
        String catTotal = getTranslation("customer.dashboard.category.total");
        String seriesSLA = getTranslation("customer.dashboard.series.sla");

        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  if (window.ApexCharts) {" +
            
            "    /* 1. Donut Chart */" +
            "    var statusEl = document.querySelector('#customer-status-chart');" +
            "    if(statusEl) { statusEl.innerHTML = ''; new window.ApexCharts(statusEl, { series: [$0, $1], labels: [$7, $8], chart: { type: 'donut', height: 250, background: 'transparent', toolbar: { show: false } }, colors: [$2, $3], legend: { position: 'bottom' }, dataLabels: { enabled: false } }).render(); }" +

            "    /* 2. Area Chart */" +
            "    var actEl = document.querySelector('#customer-activity-chart');" +
            "    if(actEl) { actEl.innerHTML = ''; new window.ApexCharts(actEl, { series: [{ name: $9, data: $4 }], chart: { type: 'area', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#3B82F6'], dataLabels: { enabled: false }, stroke: { curve: 'smooth', width: 2 }, xaxis: { categories: $5 }, fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.7, opacityTo: 0.1, stops: [0, 90, 100] } } }).render(); }" +

            "    /* 3. Bar Chart (Detay) */" +
            "    var detailEl = document.querySelector('#customer-detail-chart');" +
            "    if(detailEl) { detailEl.innerHTML = ''; new window.ApexCharts(detailEl, { series: [{ name: $10, data: [$0, $1, $6] }], chart: { type: 'bar', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#8B5CF6'], plotOptions: { bar: { borderRadius: 4, horizontal: true } }, dataLabels: { enabled: false }, xaxis: { categories: [$11, $12, $13] } }).render(); }" +
            
            "    /* 4. SLA (Performans) Line Chart (YENİ EKLENDİ) */" +
            "    var slaEl = document.querySelector('#customer-sla-chart');" +
            "    if(slaEl) { slaEl.innerHTML = ''; new window.ApexCharts(slaEl, { series: [{ name: $15, data: $14 }], chart: { type: 'line', height: 250, background: 'transparent', toolbar: { show: false } }, colors: ['#10B981'], stroke: { curve: 'smooth', width: 3 }, markers: { size: 4 }, xaxis: { categories: $5 }, dataLabels: { enabled: true } }).render(); }" +
            
            "  }" +
            "}, 500);", 
            dataKapatilan, dataİslemde, color1, color2,
            acilanTalepTrend, trendGunleri, (double) totalRequests,
            labelResolved, labelInProgress, seriesRequests, seriesCount,
            catResolved, catPending, catTotal,
            slaTrend, seriesSLA
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