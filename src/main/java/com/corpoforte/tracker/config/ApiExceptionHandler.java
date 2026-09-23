package com.corpoforte.tracker.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Todo erro da API sai como ProblemDetail (RFC 7807), num formato so' pros
 * dois clientes. Os 401/403 dos filtros de seguranca ficam com
 * ProblemaDeSegurancaApi, porque nascem antes de existir controller.
 *
 * Por que um advice GLOBAL que filtra pelo caminho, e nao um advice
 * restrito a @RestController: rota inexistente (NoResourceFoundException)
 * e metodo errado (HttpRequestMethodNotSupportedException) acontecem antes
 * de o Spring escolher um controller, e um advice restrito nunca os ve.
 * Sem tratamento aqui, eles viravam um redirecionamento pro login do Google
 * (a pagina /error mora na chain web, que exige sessao).
 *
 * Requisicao que nao e' da API tem a excecao relancada: o Spring segue pro
 * proximo resolver como se este advice nao existisse, e as telas web
 * continuam com o tratamento de erro de sempre.
 *
 * O mapeamento excecao -> status e' o do proprio Spring
 * (ResponseEntityExceptionHandler, usado por delegacao), com o texto do
 * "detail" em portugues via messages.properties.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final MapeamentoPadrao mapeamentoPadrao = new MapeamentoPadrao();

    public ApiExceptionHandler(MessageSource messageSource) {
        mapeamentoPadrao.setMessageSource(messageSource);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratar(Exception excecao, HttpServletRequest request, WebRequest webRequest)
            throws Exception {
        // excecao de seguranca lancada dentro do controller volta pro Spring
        // Security, que responde pelo ProblemaDeSegurancaApi
        if (!ehDaApi(request) || excecao instanceof AuthenticationException
                || excecao instanceof AccessDeniedException) {
            throw excecao;
        }

        try {
            return mapeamentoPadrao.handleException(excecao, webRequest);
        } catch (Exception naoMapeada) {
            log.error("Erro inesperado na API: {} {}", request.getMethod(), request.getRequestURI(), naoMapeada);
            // sem a mensagem da excecao: pode carregar SQL, nome de tabela,
            // dado de outro usuario
            return ResponseEntity.internalServerError()
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado"));
        }
    }

    private static boolean ehDaApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    /** Campo do corpo que falhou na Bean Validation, na ordem em que o
     * validador reportou. */
    public record CampoInvalido(String campo, String mensagem) {
    }

    /**
     * Unica diferenca pro padrao do Spring: erro de validacao lista os
     * campos invalidos (propriedade "campos"). Sem isso o cliente so'
     * saberia que "algum campo" esta errado.
     */
    private static class MapeamentoPadrao extends ResponseEntityExceptionHandler {

        @Override
        protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excecao,
                                                                      HttpHeaders headers,
                                                                      HttpStatusCode status,
                                                                      WebRequest request) {
            ProblemDetail problema = excecao.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
            List<CampoInvalido> campos = excecao.getFieldErrors().stream()
                    .map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                    .toList();
            problema.setProperty("campos", campos);
            return handleExceptionInternal(excecao, problema, headers, status, request);
        }
    }
}
