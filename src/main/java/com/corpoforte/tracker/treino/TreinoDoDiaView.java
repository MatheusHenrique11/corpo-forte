package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.time.LocalDate;
import java.util.List;

public record TreinoDoDiaView(LocalDate data, List<TreinoItemView> itens, List<MovimentoPadrao> movimentosSemOpcao,
                               CicloStatus ciclo) {
}
