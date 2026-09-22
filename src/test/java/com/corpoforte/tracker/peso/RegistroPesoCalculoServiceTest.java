package com.corpoforte.tracker.peso;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RegistroPesoCalculoServiceTest {

    private final RegistroPesoCalculoService service = new RegistroPesoCalculoService();

    @Test
    void comparaMediaDaSemanaAtualComAMediaDaSemanaAnterior() {
        // semana de 01/06/2026 (segunda) a 07/06/2026: media 80.0
        // semana de 08/06/2026 (segunda) a 14/06/2026: media 79.0 (perdeu peso)
        List<RegistroPesoPonto> registros = List.of(
                new RegistroPesoPonto(LocalDate.of(2026, 6, 1), 80.0),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 3), 80.4),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 7), 79.6),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 8), 79.5),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 10), 79.0),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 14), 78.5)
        );

        TendenciaSemanal resultado = service.calcularTendencia(registros);

        assertThat(resultado.inicioSemanaAtual()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(resultado.mediaSemanaAtual()).isCloseTo(79.0, within(0.01));
        assertThat(resultado.inicioSemanaAnterior()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(resultado.mediaSemanaAnterior()).isCloseTo(80.0, within(0.01));
        assertThat(resultado.variacaoKg()).isCloseTo(-1.0, within(0.01));
        assertThat(resultado.direcao()).isEqualTo(DirecaoTendencia.DESCENDO);
    }

    @Test
    void semSemanaAnteriorFicaSemComparativo() {
        List<RegistroPesoPonto> registros = List.of(
                new RegistroPesoPonto(LocalDate.of(2026, 6, 8), 79.5),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 10), 79.0)
        );

        TendenciaSemanal resultado = service.calcularTendencia(registros);

        assertThat(resultado.mediaSemanaAnterior()).isNull();
        assertThat(resultado.variacaoKg()).isNull();
        assertThat(resultado.direcao()).isEqualTo(DirecaoTendencia.SEM_COMPARATIVO);
    }

    @Test
    void variacaoPequenaContaComoEstavel() {
        List<RegistroPesoPonto> registros = List.of(
                new RegistroPesoPonto(LocalDate.of(2026, 6, 1), 80.0),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 8), 80.02)
        );

        TendenciaSemanal resultado = service.calcularTendencia(registros);

        assertThat(resultado.direcao()).isEqualTo(DirecaoTendencia.ESTAVEL);
    }

    @Test
    void usaAsDuasSemanasMaisRecentesComDadoMesmoComLacunaEntreElas() {
        // sem nenhum registro na semana de 08/06 - a comparacao pula direto
        // pra semana de 01/06, que e' a segunda mais recente com dado
        List<RegistroPesoPonto> registros = List.of(
                new RegistroPesoPonto(LocalDate.of(2026, 6, 1), 80.0),
                new RegistroPesoPonto(LocalDate.of(2026, 6, 15), 78.0)
        );

        TendenciaSemanal resultado = service.calcularTendencia(registros);

        assertThat(resultado.inicioSemanaAtual()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(resultado.inicioSemanaAnterior()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(resultado.variacaoKg()).isCloseTo(-2.0, within(0.01));
    }
}
