package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.util.List;

public record TreinoGerado(List<ItemGerado> itens, List<MovimentoPadrao> movimentosSemOpcao) {
}
