package com.corpoforte.tracker.atividade;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Um exercicio e as series feitas nele, em ordem: [10, 10, 8] = tres
 * series. O valor e' repeticao ou segundo conforme a medida do exercicio;
 * o teto de cada medida e' conferido no service, que conhece o catalogo.
 */
public record SeriesDeExercicio(
        @NotNull(message = "Informe o exercício") Long exercicioId,
        @NotEmpty(message = "Informe ao menos uma série")
        @Size(max = 20, message = "No máximo 20 séries por exercício")
        List<@NotNull @Min(value = 1, message = "Cada série vale pelo menos 1")
             @Max(value = 3600, message = "Cada série vale no máximo 3600") Integer> series) {
}
