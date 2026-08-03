package com.example.base.ui.ProgrammerScreen;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.base.entity.RequestEntity;
import com.example.base.entity.WorkflowEntity;
import com.example.base.repository.RequestRepository;
import com.example.base.repository.WorkflowRepository;
import com.example.base.service.NotificationService;
import com.example.base.service.SystemLogService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "programmer-paneli", layout = MainLayout.class)
@RolesAllowed("PROGRAMMER") 
public class ProgrammerDashboardView extends VerticalLayout {

    private final WorkflowRepository workflowRepository;
    private final RequestRepository requestRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;

    private final Grid<WorkflowEntity> grid = new Grid<>(WorkflowEntity.class, false);
    private final Dialog detailDialog = new Dialog();

    public ProgrammerDashboardView(WorkflowRepository workflowRepository, 
                                  RequestRepository requestRepository,
                                  NotificationService notificationService, 
                                  SystemLogService systemLogService) {
        this.workflowRepository = workflowRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.systemLogService = systemLogService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0, 0, 0, 0.05)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "24px")
                .set("max-width", "1400px")
                .set("margin", "0 auto")
                .set("height", "calc(100vh - 120px)")
                .set("display", "flex")
                .set("flex-direction", "column");

        configureGrid();

        grid.setWidthFull();
        grid.getStyle()
                .set("flex-grow", "1")
                .set("border-radius", "12px")
                .set("margin-top", "16px");

        mainContainer.add(buildHeader(), grid);
        add(mainContainer);
        
        refreshGrid();
    }

    private Div buildHeader() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle().set("flex-shrink", "0");

        H2 title = new H2("Programmer Görev Paneli");
        title.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-header-text-color)");

        Paragraph subtitle = new Paragraph("PO tarafından onaylanıp Backlog'a atılan işleri buradan takip edip sonuçlandırın.");
        subtitle.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

        header.add(title, subtitle);
        return header;
    }

    private void configureGrid() {
        grid.addColumn(workflow -> workflow.getRequest() != null ? workflow.getRequest().getRequestId() : "-")
                .setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);

        grid.addColumn(workflow -> workflow.getRequest() != null ? workflow.getRequest().getTitle() : "Bilinmiyor")
                .setHeader("Görev Başlığı").setFlexGrow(2);

        grid.addColumn(workflow -> {
            if (workflow.getAssignedAt() != null) {
                return workflow.getAssignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            }
            return "-";
        }).setHeader("Görevlendirme Tarihi").setAutoWidth(true);

        grid.addComponentColumn(this::createPriorityBadge)
                .setHeader("Öncelik").setAutoWidth(true);

        grid.addComponentColumn(this::createActionColumn)
                .setHeader("Durum Güncelle").setAutoWidth(true).setFlexGrow(1);
                
        grid.addComponentColumn(workflow -> {
            Button detailBtn = new Button("Detay Oku", VaadinIcon.INFO_CIRCLE.create());
            detailBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            detailBtn.addClickListener(e -> showDetails(workflow));
            return detailBtn;
        }).setHeader("İçerik").setAutoWidth(true);

        // BURADA DEĞİŞİKLİK YAPILDI: WorkflowEntity üzerinden SLA rozeti çağrılıyor
        grid.addComponentColumn(this::createSlaBadge).setHeader("SLA Durumu").setAutoWidth(true).setFlexGrow(0);
    }

    // BURADA DEĞİŞİKLİK YAPILDI: Parametre WorkflowEntity olarak değiştirildi
    private Badge createSlaBadge(WorkflowEntity workflow) {
        RequestEntity request = workflow.getRequest();
        
        if (request == null) {
             return new Badge("-");
        }

        if ("KAPATILDI".equals(request.getStatus())) {
            Badge closedBadge = new Badge("Tamamlandı");
            closedBadge.addThemeVariants(BadgeVariant.CONTRAST); 
            return closedBadge;
        }

        long hoursElapsed = ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());

        long slaLimitHours = 24; 
        long warningLimitHours = (long) (slaLimitHours * 0.75);

        if (hoursElapsed >= slaLimitHours) {
            Badge ihlalBadge = new Badge("İHLAL (" + hoursElapsed + "s)");
            ihlalBadge.addThemeVariants(BadgeVariant.ERROR);
            ihlalBadge.getElement().setProperty("title", "SLA Süresi Aşıldı!");
            return ihlalBadge;
        } else if (hoursElapsed >= warningLimitHours) {
            Badge uyariBadge = new Badge("YAKLAŞIYOR (" + hoursElapsed + "s)");
            uyariBadge.addThemeVariants(BadgeVariant.WARNING);
            uyariBadge.getElement().setProperty("title", "SLA İhlaline Az Kaldı!");
            return uyariBadge;
        } else {
            Badge normalBadge = new Badge("NORMAL (" + hoursElapsed + "s)");
            normalBadge.addThemeVariants(BadgeVariant.SUCCESS);
            return normalBadge;
        }
    }

    private Span createPriorityBadge(WorkflowEntity workflow) {
        Span badge = new Span();
        if (workflow.getRequest() != null && workflow.getRequest().getPrioritization() != null) {
            int score = workflow.getRequest().getPrioritization().getPriorityScore();
            if (score >= 999) {
                badge.setText("ACİL/GÜVENLİK");
                badge.getElement().getThemeList().add("badge error");
            } else if (score >= 100) {
                badge.setText("KRİTİK");
                badge.getElement().getThemeList().add("badge error primary");
            } else if (score >= 50) {
                badge.setText("YÜKSEK");
                badge.getElement().getThemeList().add("badge warning");
            } else {
                badge.setText("NORMAL");
                badge.getElement().getThemeList().add("badge success");
            }
        } else {
            badge.setText("BELİRSİZ");
            badge.getElement().getThemeList().add("badge contrast");
        }
        return badge;
    }

    private HorizontalLayout createActionColumn(WorkflowEntity workflow) {
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems("BACKLOG", "IN DEVELOPMENT", "TEST", "DONE");
        
        String currentStatus = workflow.getWorkflowStatus();
        statusCombo.setValue(currentStatus != null ? currentStatus : "BACKLOG");
        statusCombo.setWidth("180px");

        Button saveBtn = new Button(VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        saveBtn.addClickListener(e -> updateWorkflowStatus(workflow, statusCombo.getValue()));

        HorizontalLayout layout = new HorizontalLayout(statusCombo, saveBtn);
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    private void updateWorkflowStatus(WorkflowEntity workflow, String newStatus) {
        if (newStatus == null || newStatus.equals(workflow.getWorkflowStatus())) {
            return;
        }

        try {
            workflow.setWorkflowStatus(newStatus);
            workflowRepository.save(workflow);

            RequestEntity request = workflow.getRequest();
            
            if ("DONE".equals(newStatus) && request != null) {
                request.setStatus("KAPATILDI");
                requestRepository.save(request);
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String devEmail = (auth != null) ? auth.getName() : "";
            
            systemLogService.log("Programmer (" + devEmail + ") Görev ID: " + workflow.getTaskId() + " durumunu güncelledi: " + newStatus); 
            if (request != null && request.getCustomer() != null) {
                String notifMessage = "Talebinizin yazılım süreci güncellendi. Yeni durum: " + newStatus;
                notificationService.notifyUser(request.getCustomer().getUserId(), "Yazılım Süreci", notifMessage);
            }

            Notification.show("Durum başarıyla güncellendi!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            refreshGrid();

        } catch (Exception ex) {
            Notification.show("Hata: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showDetails(WorkflowEntity workflow) {
        detailDialog.removeAll();
        detailDialog.setHeaderTitle("Görev Detayları");
        detailDialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        if (workflow.getRequest() != null) {
            Span title = new Span("Başlık: " + workflow.getRequest().getTitle());
            title.getStyle().set("font-weight", "bold");
            
            TextArea desc = new TextArea("Sorun / İstek Detayı");
            desc.setValue(workflow.getRequest().getDescription() != null ? workflow.getRequest().getDescription() : "");
            desc.setReadOnly(true);
            desc.setWidthFull();
            desc.setMinHeight("150px");
            
            content.add(title, desc);
        } else {
            content.add(new Span("Bu göreve bağlı detay bulunamadı."));
        }

        Button closeBtn = new Button("Kapat", e -> detailDialog.close());
        detailDialog.getFooter().add(closeBtn);
        
        detailDialog.add(content);
        detailDialog.open();
    }

    private void refreshGrid() {
        grid.setItems(workflowRepository.findAllWithRequests()); 
    }
}