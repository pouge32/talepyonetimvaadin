package com.example.base.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    public AuthenticationEventListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void authenticationFailed(AuthenticationFailureBadCredentialsEvent event) {
        String email = (String) event.getAuthentication().getPrincipal();
        if (email != null) {
            loginAttemptService.loginFailed(email);
        }
    }

    @EventListener
    public void authenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        if (email != null) {
            loginAttemptService.loginSucceeded(email);
        }
    }
}