package com.corpoforte.tracker.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Duas chains, cada uma com o modelo de autenticacao do seu cliente.
 *
 * API (/api/**, Fase 10): stateless, access token proprio no cabecalho
 * Authorization. Sem CSRF porque nao ha cookie de sessao pra um site
 * terceiro pegar carona: o navegador nunca anexa o token sozinho. Sem
 * sessao: um cookie de sessao web valido NAO autentica a API
 * (ApiSegurancaIT), senao a API herdaria o risco de CSRF que acabou de
 * desligar.
 *
 * Web (todo o resto): toda rota exige login (nenhuma pagina publica -
 * app e' uso pessoal, sem necessidade de landing page). CSRF fica ligado
 * de proposito (nao e' desligado por conveniencia):
 * thymeleaf-extras-springsecurity6 injeta o token automaticamente em todo
 * <form th:action=...> existente, entao nenhum template precisou mudar por
 * causa disso.
 */
@Configuration
public class SecurityConfig {

    /** Unicas rotas da API sem access token: sao elas que o emitem. */
    private static final String[] ROTAS_DE_AUTENTICACAO = {
            "/api/v1/auth/google", "/api/v1/auth/refresh", "/api/v1/auth/logout"};

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      JwtDecoder jwtDecoder,
                                                      ProblemaDeSegurancaApi problemas,
                                                      @Value("${app.cors.origins:}") List<String> origensCors)
            throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, ROTAS_DE_AUTENTICACAO).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsDaApi(origensCors)))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .bearerTokenResolver(ignorandoTokenNasRotasDeAutenticacao())
                        .authenticationEntryPoint(problemas)
                        .accessDeniedHandler(problemas))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(problemas)
                        .accessDeniedHandler(problemas));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Especificacao OpenAPI e Swagger UI: so' existem no
                        // perfil dev (application-dev.yml); fora dele essas
                        // rotas dao 404. Abertas pra gerar cliente a partir
                        // da especificacao sem precisar de sessao.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

        return http.build();
    }

    /**
     * Cliente HTTP costuma anexar o access token em toda requisicao, inclusive
     * no /auth/refresh - e e' justamente quando o token ja expirou que o
     * refresh e' chamado. Sem isto, o filtro de bearer tentaria validar esse
     * token expirado e responderia 401 antes de a rota (que e' permitAll)
     * rodar: o cliente nunca conseguiria renovar a sessao.
     */
    private static BearerTokenResolver ignorandoTokenNasRotasDeAutenticacao() {
        DefaultBearerTokenResolver padrao = new DefaultBearerTokenResolver();
        return request -> ehRotaDeAutenticacao(request) ? null : padrao.resolve(request);
    }

    private static boolean ehRotaDeAutenticacao(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return List.of(ROTAS_DE_AUTENTICACAO).contains(caminho);
    }

    /**
     * So' as origens de APP_CORS_ORIGINS (as aplicacoes web que consomem a
     * API). Lista vazia = nenhuma origem externa: fechado por padrao. Cliente
     * que nao roda em navegador nao passa por CORS, entao nao entra aqui.
     *
     * Nao e' um @Bean de proposito: um UrlBasedCorsConfigurationSource no
     * contexto e' aplicado automaticamente pelo Spring Security a TODAS as
     * chains, e as telas web nao devem aceitar requisicao de outra origem.
     */
    private static CorsConfigurationSource corsDaApi(List<String> origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens.stream().filter(origem -> !origem.isBlank()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
