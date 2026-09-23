package com.corpoforte.tracker.api;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorTest {

    /** O Postgres guarda microssegundos; o cursor tem que devolver a
     * posicao exata, senao o keyset pula ou repete o item da fronteira. */
    @Test
    void instanteComFracaoDeSegundoSobreviveAIdaEVolta() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 9, 23, 15, 18, 21, 792_123_000);

        Cursor lido = Cursor.decodificar(Cursor.apos(criadoEm, 42).codificar());

        assertThat(lido.comoInstante()).isEqualTo(criadoEm);
        assertThat(lido.id()).isEqualTo(42);
    }

    @Test
    void dataSobreviveAIdaEVolta() {
        Cursor lido = Cursor.decodificar(Cursor.apos(LocalDate.of(2026, 5, 4), 7).codificar());

        assertThat(lido.comoData()).isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    void semCursorEPrimeiraPagina() {
        assertThat(Cursor.decodificar(null)).isNull();
        assertThat(Cursor.decodificar("")).isNull();
    }

    @Test
    void cursorMalFormadoDa400() {
        assertInvalido(() -> Cursor.decodificar("%%%nao-e-base64"));
        assertInvalido(() -> Cursor.decodificar(base64("sem-separador")));
        assertInvalido(() -> Cursor.decodificar(base64("2026-01-01|nao-e-numero")));
        // cursor de data usado onde se espera instante (ou o contrario)
        assertInvalido(() -> Cursor.decodificar(base64("texto-qualquer|1")).comoInstante());
        assertInvalido(() -> Cursor.decodificar(base64("texto-qualquer|1")).comoData());
    }

    private static String base64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertInvalido(ThrowingCallable chamada) {
        assertThatThrownBy(chamada).isInstanceOfSatisfying(ResponseStatusException.class,
                erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
