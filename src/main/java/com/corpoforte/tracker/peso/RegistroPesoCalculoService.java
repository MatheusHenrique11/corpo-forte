package com.corpoforte.tracker.peso;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Peso oscila dia a dia (agua, comida, digestao), entao a tendencia compara
 * a media de uma semana de pesagens com a media da semana anterior, nao
 * pesagem isolada contra pesagem isolada. Usa as duas semanas mais recentes
 * que tem registro (nao necessariamente a semana civil de hoje), pra nao
 * mostrar "sem dado" so porque o usuario ainda nao pesou nesta semana.
 * Isolado do Controller pelo mesmo motivo dos outros calculo services: sem
 * Spring nem banco no meio, da pra testar chamando calcularTendencia(...)
 * direto.
 */
@Service
public class RegistroPesoCalculoService {

    private static final double LIMIAR_ESTAVEL_KG = 0.05;

    public TendenciaSemanal calcularTendencia(List<RegistroPesoPonto> registros) {
        if (registros.isEmpty()) {
            throw new IllegalArgumentException("Precisa de ao menos um registro para calcular a tendencia");
        }

        Map<LocalDate, Double> mediaPorSemana = registros.stream()
                .collect(Collectors.groupingBy(
                        ponto -> inicioDaSemana(ponto.data()),
                        Collectors.averagingDouble(RegistroPesoPonto::pesoKg)));

        List<LocalDate> semanasMaisRecentesPrimeiro = mediaPorSemana.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        LocalDate inicioSemanaAtual = semanasMaisRecentesPrimeiro.get(0);
        double mediaSemanaAtual = mediaPorSemana.get(inicioSemanaAtual);

        if (semanasMaisRecentesPrimeiro.size() < 2) {
            return new TendenciaSemanal(inicioSemanaAtual, mediaSemanaAtual, null, null, null, DirecaoTendencia.SEM_COMPARATIVO);
        }

        LocalDate inicioSemanaAnterior = semanasMaisRecentesPrimeiro.get(1);
        double mediaSemanaAnterior = mediaPorSemana.get(inicioSemanaAnterior);
        double variacaoKg = mediaSemanaAtual - mediaSemanaAnterior;

        DirecaoTendencia direcao = Math.abs(variacaoKg) < LIMIAR_ESTAVEL_KG
                ? DirecaoTendencia.ESTAVEL
                : variacaoKg > 0 ? DirecaoTendencia.SUBINDO : DirecaoTendencia.DESCENDO;

        return new TendenciaSemanal(inicioSemanaAtual, mediaSemanaAtual, inicioSemanaAnterior,
                mediaSemanaAnterior, variacaoKg, direcao);
    }

    private LocalDate inicioDaSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
