package com.corpoforte.tracker.atividade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Finalizar o treino do dia. Tudo opcional: sem ajustes, cada item marcado
 * entra com a prescricao (series x repeticoes do treino gerado). ajustes
 * troca a prescricao do que foi feito diferente - quem fez 8 em vez de 10
 * registra 8.
 */
public record FinalizacaoRequisicao(
        @Min(value = 1, message = "Duração mínima: 1 minuto")
        @Max(value = 600, message = "Duração máxima: 600 minutos") Integer duracaoMinutos,
        @Min(value = 1, message = "Esforço de 1 a 10") @Max(value = 10, message = "Esforço de 1 a 10")
        Integer esforcoPercebido,
        @Size(max = 500, message = "Máximo de 500 caracteres") String notas,
        List<@Valid Ajuste> ajustes) {

    /** O que foi feito num item do treino, no lugar da prescricao. */
    public record Ajuste(
            @NotNull(message = "Informe o item") Long itemId,
            @NotEmpty(message = "Informe ao menos uma série")
            @Size(max = 20, message = "No máximo 20 séries por exercício")
            List<@NotNull @Min(value = 1, message = "Cada série vale pelo menos 1")
                 @Max(value = 3600, message = "Cada série vale no máximo 3600") Integer> series) {
    }
}
