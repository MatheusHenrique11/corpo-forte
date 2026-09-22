package com.corpoforte.tracker.peso;

import java.time.LocalDate;

public record TendenciaSemanal(
        LocalDate inicioSemanaAtual,
        double mediaSemanaAtual,
        LocalDate inicioSemanaAnterior,
        Double mediaSemanaAnterior,
        Double variacaoKg,
        DirecaoTendencia direcao
) {
}
