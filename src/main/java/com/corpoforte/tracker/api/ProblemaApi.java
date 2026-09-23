package com.corpoforte.tracker.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * Erro da API que o cliente precisa distinguir pra decidir o que mostrar
 * (ex.: "faca a avaliacao fisica primeiro"), identificado pelo "type" do
 * ProblemDetail em vez de pelo texto do "detail", que pode mudar. O type e'
 * uma URN estavel: nao aponta pra pagina nenhuma, so' identifica o caso.
 *
 * Todos sao 409: o pedido e' valido, mas o estado da conta ainda nao
 * permite atende-lo.
 */
public final class ProblemaApi {

    private static final String PREFIXO_TIPO = "urn:corpo-forte:problema:";

    private ProblemaApi() {
    }

    /** Treino do dia sem nenhuma avaliacao fisica pra calcular o volume. */
    public static ErrorResponseException avaliacaoPendente() {
        return conflito("avaliacao-pendente", "Faça a avaliação física para gerar o treino do dia.");
    }

    /** Comparacao entre avaliacoes com menos de duas no historico. */
    public static ErrorResponseException avaliacoesInsuficientes() {
        return conflito("avaliacoes-insuficientes", "A comparação precisa de pelo menos duas avaliações.");
    }

    /** Tendencia semanal sem nenhum registro de peso. */
    public static ErrorResponseException registroDePesoPendente() {
        return conflito("registro-de-peso-pendente", "Registre o peso ao menos uma vez para ver a tendência.");
    }

    private static ErrorResponseException conflito(String tipo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detalhe);
        problema.setType(URI.create(PREFIXO_TIPO + tipo));
        return new ErrorResponseException(HttpStatus.CONFLICT, problema, null);
    }
}
