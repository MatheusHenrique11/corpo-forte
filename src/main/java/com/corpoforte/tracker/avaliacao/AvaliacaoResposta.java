package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.time.LocalDate;
import java.util.List;

/**
 * Uma avaliacao fisica com o volume calculado por movimento. Espelha
 * AvaliacaoFisicaItemResultado em vez de expo-lo: o record de calculo pode
 * mudar livremente, o contrato da API so' cresce.
 */
public record AvaliacaoResposta(LocalDate data, List<ResultadoPorMovimento> itens) {

    public record ResultadoPorMovimento(MovimentoPadrao movimento, int repeticoesMaximas, int volumeTotalTreino,
                                        int volumeInicial, int incremento) {
    }

    static AvaliacaoResposta de(AvaliacaoFisica avaliacao, List<AvaliacaoFisicaItemResultado> resultado) {
        return new AvaliacaoResposta(avaliacao.getDataAvaliacao(), resultado.stream()
                .map(item -> new ResultadoPorMovimento(item.movimento(), item.repeticoesMaximas(),
                        item.volumeTotalTreino(), item.volumeInicial(), item.incremento()))
                .toList());
    }
}
