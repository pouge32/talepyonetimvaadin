package com.example.base.ui;

import com.example.base.entity.RequestEntity;
import com.example.base.service.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "talep-degerlendir", layout = MainLayout.class)
@RolesAllowed("PO")
public class TalepDegerlendirme extends VerticalLayout {

    private final RequestService requestService;
    
    private Grid<RequestEntity> grid = new Grid<>(RequestEntity.class, false);
    private ComboBox<String> urgency = new ComboBox<>("Aciliyet");
    private ComboBox<String> impact = new ComboBox<>("İş Etkisi");
    private Button scoreButton = new Button("Kaydet ve Önceliklendir");
    private Button iptal = new Button("İptal");
    private Button aktar = new Button("Yazılımcıya Aktar");
    private Dialog secim = new Dialog("Önceliklendirme");   
    
    private RequestEntity selectedRequest;

    public TalepDegerlendirme(RequestService requestService) {
        this.requestService = requestService;

        add(new H3("Bekleyen Talepler (PO Ekranı)"));

        configureGrid();
        configureComboBoxes();

        scoreButton.addClickListener(event -> evaluateRequest());
        iptal.addClickListener(event -> cancel());
        aktar.addClickListener(event -> convertToWorkflow());

        secim.getFooter().add(scoreButton, iptal, aktar);
        FormLayout formLayout = new FormLayout(urgency, impact);
        secim.add(formLayout);
        secim.setCloseOnOutsideClick(false);
        
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(RequestEntity::getRequestId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(RequestEntity::getTitle).setHeader("Başlık");
        grid.addColumn(RequestEntity::getDescription).setHeader("Detay");
        grid.addColumn(RequestEntity::getCreatedAt).setHeader("Oluşturulma Tarihi");
        grid.addColumn(RequestEntity::getStatus).setHeader("Durum");

        grid.addColumn(request -> requestService.getRequestPriority(request.getRequestId()))
            .setHeader("Öncelik Puanı").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            selectedRequest = event.getValue();
            if (selectedRequest != null) {
                secim.open(); 
            } 
        });
    }
    
    private void cancel(){
        secim.close();
    }

    private void configureComboBoxes() {
        urgency.setItems(
            "1 - Düşük: Kozmetik sorunlar, acelesi olmayan istekler.",
            "2 - Orta: İşlemi engellemeyen sorunlar, alternatif yol var.",
            "3 - Yüksek: Ana fonksiyon bozuk, iş akışı ciddi etkileniyor.",
            "4 - Çok Acil: Sistem durmuş durumda, işlem yapılamıyor."
        );

        impact.setItems(
            "1 - Düşük: Sadece tek bir kullanıcıyı etkiler.",
            "2 - Orta: Küçük bir ekibi veya az sayıda müşteriyi etkiler.",
            "3 - Yüksek: Bütün bir departmanı etkiler.",
            "4 - Kritik: Tüm şirketi veya tüm müşterileri etkiler."
        );
    }

    private void evaluateRequest() {
        if (selectedRequest != null && urgency.getValue() != null && impact.getValue() != null) {
            try {
                int urgencyPuan = Integer.parseInt(urgency.getValue().substring(0, 1));
                int impactPuan = Integer.parseInt(impact.getValue().substring(0, 1));

                if (urgencyPuan < 1 || urgencyPuan > 5 || impactPuan < 1 || impactPuan > 5) {
                    Notification error = Notification.show(
                        "Aciliyet ve Etki değerleri 1 ile 5 arasında olmalıdır.",
                        3000, Notification.Position.MIDDLE);
                    error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return; 
                }

                requestService.prioritizeRequest(selectedRequest.getRequestId(), urgencyPuan, impactPuan);

                Notification.show("Talep başarıyla önceliklendirildi!", 3000, Notification.Position.TOP_CENTER);

                urgency.clear();
                impact.clear();
                selectedRequest = null;

                secim.close();
                refreshGrid();

            } catch (Exception e) {
                Notification.show("Hata: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
            }
        } else {
            Notification.show("Lütfen listeden bir talep seçin ve puanları belirleyin.", 3000, Notification.Position.MIDDLE);
        }
    }

    private void convertToWorkflow() {
        if (selectedRequest != null) {
            try {
                requestService.goreveDonustur(selectedRequest);

                Notification success = Notification.show(
                    "Talep başarıyla göreve dönüştürüldü!", 3000, Notification.Position.TOP_CENTER);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS); 

                selectedRequest = null;
                secim.close();

                grid.getDataProvider().refreshAll(); 
                refreshGrid();

            } catch (Exception e) {
                Notification error = Notification.show(
                    "Hata: " + e.getMessage(), 4000, Notification.Position.TOP_CENTER);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            Notification.show("Lütfen listeden bir talep seçin.", 3000, Notification.Position.MIDDLE);
        }
    }

    private void refreshGrid() {
        grid.setItems(requestService.getNewRequests());
    }
}