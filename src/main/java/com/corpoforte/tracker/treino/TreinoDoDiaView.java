package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.util.List;

public record TreinoDoDiaView(List<TreinoItemView> itens, List<MovimentoPadrao> movimentosSemOpcao,
                               CicloStatus ciclo) {
}
