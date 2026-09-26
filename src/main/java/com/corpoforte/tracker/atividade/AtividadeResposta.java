package com.corpoforte.tracker.atividade;

import com.corpoforte.tracker.exercicio.Medida;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma atividade do diario. exercicios agrupa as series na ordem em que
 * foram feitas: series seguidas do mesmo exercicio formam um bloco
 * (voltar a um exercicio depois de outro abre um bloco novo). total soma
 * as series do bloco, na medida do exercicio.
 */
public record AtividadeResposta(Long id, LocalDate data, OrigemAtividade origem, Integer duracaoMinutos,
                                Integer esforcoPercebido, String notas, Instant criadoEm,
                                List<Bloco> exercicios) {

    public record Bloco(ExercicioDoBloco exercicio, List<Integer> series, int total) {
    }

    public record ExercicioDoBloco(Long id, String nome, MovimentoPadrao movimento, Medida medida) {
    }
}
