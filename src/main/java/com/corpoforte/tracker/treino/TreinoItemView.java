package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

public record TreinoItemView(Long itemId, MovimentoPadrao movimento, String exercicioNome, int series,
                              int repeticoes, boolean concluido) {
}
