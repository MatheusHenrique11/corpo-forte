package com.corpoforte.tracker.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * Posicao do ultimo item de uma pagina (keyset): o valor da coluna de
 * ordenacao mais o id, que desempata itens com o mesmo valor. A proxima
 * pagina comeca DEPOIS dessa posicao, em vez de "pular N linhas" (offset):
 * com offset, um post novo chegando entre uma pagina e outra empurra tudo
 * uma posicao e o cliente ve um item repetido.
 *
 * Pro cliente o cursor e' opaco (base64url): ele so' devolve o que
 * recebeu. Um cursor adulterado nao da acesso a nada - toda consulta
 * paginada continua filtrada pelo usuario ou pelo que ja e' publico - e
 * cursor mal formado da 400.
 */
public record Cursor(String posicao, long id) {

    private static final String SEPARADOR = "|";

    public static Cursor apos(LocalDateTime criadoEm, long id) {
        return new Cursor(criadoEm.toString(), id);
    }

    public static Cursor apos(LocalDate data, long id) {
        return new Cursor(data.toString(), id);
    }

    /** null quando o cliente nao mandou cursor (primeira pagina). */
    public static Cursor decodificar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            String texto = new String(Base64.getUrlDecoder().decode(valor), StandardCharsets.UTF_8);
            int separador = texto.lastIndexOf(SEPARADOR);
            return new Cursor(texto.substring(0, separador), Long.parseLong(texto.substring(separador + 1)));
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw invalido();
        }
    }

    public String codificar() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((posicao + SEPARADOR + id).getBytes(StandardCharsets.UTF_8));
    }

    public LocalDateTime comoInstante() {
        try {
            return LocalDateTime.parse(posicao);
        } catch (DateTimeParseException e) {
            throw invalido();
        }
    }

    public LocalDate comoData() {
        try {
            return LocalDate.parse(posicao);
        } catch (DateTimeParseException e) {
            throw invalido();
        }
    }

    private static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor inválido");
    }
}
