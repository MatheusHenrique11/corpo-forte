package com.corpoforte.tracker.avaliacao;

public record AvaliacaoFisicaItemResultado(
        MovimentoPadrao movimento,
        int repeticoesMaximas,
        int volumeTotalTreino,
        int volumeInicial,
        int incremento
) {
}
