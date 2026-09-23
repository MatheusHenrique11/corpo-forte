package com.corpoforte.tracker.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os dois caminhos do advice que um teste pela API nao alcanca sem criar
 * um controller so' pra quebrar: o 500 generico e a devolucao da excecao
 * quando a requisicao e' das telas web.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler(new StaticMessageSource());

    @Test
    void erroInesperadoNaApiViraProblemDetail500SemAMensagemInterna() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/qualquer");

        ResponseEntity<Object> resposta = handler.tratar(
                new IllegalStateException("select * from usuario where email = 'alguem@exemplo.com'"),
                request, new ServletWebRequest(request));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody()).isInstanceOfSatisfying(ProblemDetail.class, problema -> {
            assertThat(problema.getDetail()).isEqualTo("Erro interno inesperado");
            assertThat(problema.toString()).doesNotContain("usuario", "alguem@exemplo.com");
        });
    }

    @Test
    void responseStatusExceptionDaApiMantemStatusEMotivo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/qualquer");

        ResponseEntity<Object> resposta = handler.tratar(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado"),
                request, new ServletWebRequest(request));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(((ProblemDetail) resposta.getBody()).getDetail()).isEqualTo("Post não encontrado");
    }

    /** Telas web: a excecao volta intacta e o Spring segue pro tratamento
     * de erro de sempre, como se o advice nao existisse. */
    @Test
    void requisicaoDasTelasWebRelancaAMesmaExcecao() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/treino-do-dia/itens/1/concluir");
        ResponseStatusException excecao = new ResponseStatusException(HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> handler.tratar(excecao, request, new ServletWebRequest(request)))
                .isSameAs(excecao);
    }

    /** Negacao de acesso dentro de um controller da API volta pro Spring
     * Security (401/403 com WWW-Authenticate), em vez de virar 500 aqui. */
    @Test
    void excecaoDeSegurancaNaApiVoltaProSpringSecurity() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/qualquer");
        AccessDeniedException excecao = new AccessDeniedException("negado");

        assertThatThrownBy(() -> handler.tratar(excecao, request, new ServletWebRequest(request)))
                .isSameAs(excecao);
    }
}
