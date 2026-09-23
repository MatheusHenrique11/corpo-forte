package com.corpoforte.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Primeira classe de configuracao do projeto. Toda rota exige login
 * (nenhuma pagina publica ainda - app e' uso pessoal, sem necessidade de
 * landing page). CSRF fica ligado de proposito (nao e' desligado por
 * conveniencia): thymeleaf-extras-springsecurity6 injeta o token
 * automaticamente em todo <form th:action=...> existente, entao nenhum
 * template precisou mudar por causa disso.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

        return http.build();
    }
}
