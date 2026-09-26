package com.corpoforte.tracker.atividade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Treino livre: a sessao montada pela pessoa. Sem data, vale hoje. */
public record AtividadeLivreRequisicao(
        @PastOrPresent(message = "A data não pode ser no futuro") LocalDate data,
        @Min(value = 1, message = "Duração mínima: 1 minuto")
        @Max(value = 600, message = "Duração máxima: 600 minutos") Integer duracaoMinutos,
        @Min(value = 1, message = "Esforço de 1 a 10") @Max(value = 10, message = "Esforço de 1 a 10")
        Integer esforcoPercebido,
        @Size(max = 500, message = "Máximo de 500 caracteres") String notas,
        @NotEmpty(message = "Informe ao menos um exercício")
        @Size(max = 30, message = "No máximo 30 exercícios") List<@Valid SeriesDeExercicio> exercicios) {
}
