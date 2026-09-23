package com.corpoforte.tracker.peso;

import java.time.LocalDate;

/**
 * Media da semana mais recente com pesagem contra a semana anterior com
 * pesagem (Fase 3). Os campos da semana anterior e a variacao vem null
 * quando so' existe uma semana com dado (direcao SEM_COMPARATIVO).
 */
public record TendenciaResposta(LocalDate inicioSemanaAtual, double mediaSemanaAtual,
                                LocalDate inicioSemanaAnterior, Double mediaSemanaAnterior,
                                Double variacaoKg, DirecaoTendencia direcao) {

    static TendenciaResposta de(TendenciaSemanal tendencia) {
        return new TendenciaResposta(tendencia.inicioSemanaAtual(), tendencia.mediaSemanaAtual(),
                tendencia.inicioSemanaAnterior(), tendencia.mediaSemanaAnterior(),
                tendencia.variacaoKg(), tendencia.direcao());
    }
}
