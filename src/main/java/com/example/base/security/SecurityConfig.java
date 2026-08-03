package com.example.base.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.ui.LoginScreen.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/images/**").permitAll()
        );

        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class);
        }).formLogin(form -> form
            .failureHandler((request, response, exception) -> {
                String errorCode = "invalid";
                if (exception instanceof org.springframework.security.authentication.DisabledException) {
                    String msg = exception.getMessage();
                    if (msg != null && msg.contains("yasaklanmıştır")) {
                        errorCode = "banned";
                    } else if (msg != null && msg.contains("onaylanmadı")) {
                        errorCode = "pending";
                    } else if (msg != null && msg.contains("reddedilmiştir")) {
                        errorCode = "rejected";
                    }
                }
                response.sendRedirect("/login?error=" + errorCode);
            })
        );
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/images/**", "/icons/**", "/styles/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            UserEntity userEntity = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

            if (userEntity.isBanned() != null && userEntity.isBanned()) {
                throw new org.springframework.security.authentication.DisabledException("Bu hesap yasaklanmıştır.");
            }
            String status = userEntity.getStatus();
            if ("ONAY_BEKLIYOR".equals(status)) {
                throw new org.springframework.security.authentication.DisabledException("Hesabınız henüz destek ekibi tarafından onaylanmadı!");
            }
            if ("REDDEDILDI".equals(status)) {
                throw new org.springframework.security.authentication.DisabledException("Kayıt başvurunuz reddedilmiştir.");
            }

            return User.withUsername(userEntity.getEmail())
                    .password(userEntity.getPasswordHash())
                    .roles(userEntity.getRole().name())
                    .build();
        };
    }
}