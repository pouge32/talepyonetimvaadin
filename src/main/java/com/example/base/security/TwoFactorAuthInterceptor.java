/*package com.example.base.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.base.ui.MainScreen.OtpVerificationView;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

@Component
public class TwoFactorAuthInterceptor implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            uiEvent.getUI().addBeforeEnterListener(enterEvent -> {
                
                if (enterEvent.getNavigationTarget().equals(OtpVerificationView.class)) {
                    return;
                }

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                    
                    boolean requiresOtp = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                            a.getAuthority().equals("ROLE_PO") || 
                            a.getAuthority().equals("ROLE_HELPDESK") || 
                            a.getAuthority().equals("ROLE_PROGRAMMER") || 
                            a.getAuthority().equals("ROLE_GODPANEL"));

                    Object passed = VaadinSession.getCurrent().getAttribute("2FA_PASSED");

                    if (requiresOtp && passed == null) {
                        enterEvent.forwardTo(OtpVerificationView.class);
                    }
                }
            });
        });
    }
}*/