package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

public record AvaliacaoFisicaItemResultado(
        MovimentoPadrao movimento,
        int repeticoesMaximas,
        int volumeTotalTreino,
        int volumeInicial,
        int incremento
) {
}
