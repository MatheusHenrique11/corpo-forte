package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

public record ComparacaoItem(MovimentoPadrao movimento, int repsAnterior, int repsAtual, int diferenca) {
}
