package com.example.base.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.base.entity.UserEntity;
import com.example.base.repository.UserRepository;
import com.example.base.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class);
        });
        return http.build();
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

            return User.withUsername(userEntity.getEmail())
                    .password(userEntity.getPasswordHash())
                    .roles(userEntity.getRole().name())
                    .build();
        };
    }

    
    
}