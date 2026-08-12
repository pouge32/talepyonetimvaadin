package com.example.base.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.base.service.SettingsService;
import com.example.base.ui.MainScreen.BakimView;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

@Component
public class MaintenanceInterceptor implements VaadinServiceInitListener {

    private final SettingsService settingsService;

    public MaintenanceInterceptor(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            uiEvent.getUI().addBeforeEnterListener(this::beforeEnter);
        });
    }

    private void beforeEnter(BeforeEnterEvent event) {
        String path = event.getLocation().getPath();

        if (path.contains("login") || 
            path.contains("bakim") || 
            path.contains("2fa") || 
            path.contains("kayit-ol") || 
            path.contains("sifremi-unuttum")) {
            return; 
        }

        if (settingsService.isMaintenanceMode()) {
            boolean isAdmin = false;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }

            if (!isAdmin) {
                event.rerouteTo(BakimView.class);
            }
        }
    }
}