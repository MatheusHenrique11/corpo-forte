package com.corpoforte.tracker.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegraUsernameTest {

    @ParameterizedTest
    @ValueSource(strings = {"joao", "joao.silva", "joao_silva_1", "abc", "a23456789012345678901234567890"})
    void formatoPermitido(String username) {
        assertThat(RegraUsername.problemaSemConsultarBanco(username)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a234567890123456789012345678901", "Joao", "joao-silva", "joao silva", "joão", ""})
    void formatoInvalido(String username) {
        assertThat(RegraUsername.problemaSemConsultarBanco(username)).contains(MotivoUsernameIndisponivel.FORMATO_INVALIDO);
    }

    @Test
    void nomesDoSistemaSaoReservados() {
        assertThat(RegraUsername.problemaSemConsultarBanco("admin")).contains(MotivoUsernameIndisponivel.RESERVADO);
        assertThat(RegraUsername.problemaSemConsultarBanco("api")).contains(MotivoUsernameIndisponivel.RESERVADO);
        assertThat(RegraUsername.problemaSemConsultarBanco("feed")).contains(MotivoUsernameIndisponivel.RESERVADO);
    }
}
