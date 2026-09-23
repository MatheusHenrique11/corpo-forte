package com.corpoforte.tracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * 401/403 da chain da API em ProblemDetail, no mesmo formato de qualquer
 * outro erro da API (ApiExceptionHandler). Esses dois nascem nos filtros do
 * Spring Security, antes de existir controller, entao o @RestControllerAdvice
 * nunca os ve - sem isto, o cliente receberia corpo vazio so' nesses casos.
 *
 * O status e o cabecalho WWW-Authenticate (RFC 6750) continuam vindo das
 * implementacoes padrao de bearer token; aqui so' se acrescenta o corpo.
 */
@Component
public class ProblemaDeSegurancaApi implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final AccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();
    private final ObjectMapper objectMapper;

    public ProblemaDeSegurancaApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excecao) throws IOException, ServletException {
        bearerEntryPoint.commence(request, response, excecao);
        // token presente mas recusado (assinatura, expiracao, emissor) x
        // nenhum token: o cliente reage diferente a cada um
        String detalhe = excecao instanceof OAuth2AuthenticationException
                ? "Token de acesso inválido ou expirado"
                : "Token de acesso ausente";
        escrever(request, response, HttpStatus.UNAUTHORIZED, detalhe);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excecao) throws IOException, ServletException {
        bearerAccessDenied.handle(request, response, excecao);
        escrever(request, response, HttpStatus.FORBIDDEN, "Acesso negado");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        // sem parametro charset, igual aos ProblemDetail do Spring MVC: JSON
        // e' UTF-8 por especificacao, e o ObjectMapper escreve os bytes assim
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
