package com.aegis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.and())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/metrics").permitAll()
                .requestMatchers("/api/customer/**").permitAll()
                .requestMatchers("/api/insights/**").permitAll()
                .requestMatchers("/api/kb/**").permitAll()
                .requestMatchers("/api/evals/**").permitAll()
                .requestMatchers("/api/ingest/**").permitAll()
                .requestMatchers("/api/triage/**").permitAll()
                .requestMatchers("/api/action/**").permitAll()
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        
        return http.build();
    }
}
