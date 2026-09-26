package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.Medida;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;

public record TreinoItemView(Long itemId, MovimentoPadrao movimento, Long exercicioId, String exercicioNome,
                              Medida medida, int series, int repeticoes, boolean concluido) {
}
