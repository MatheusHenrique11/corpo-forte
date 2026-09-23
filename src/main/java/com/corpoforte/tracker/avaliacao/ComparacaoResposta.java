package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.time.LocalDate;
import java.util.List;

/** Avaliacao mais recente contra a anterior, movimento a movimento
 * ("10 -> 15, +5"). */
public record ComparacaoResposta(LocalDate dataAtual, LocalDate dataAnterior, List<DiferencaPorMovimento> itens) {

    public record DiferencaPorMovimento(MovimentoPadrao movimento, int repsAnterior, int repsAtual, int diferenca) {
    }

    static ComparacaoResposta de(ComparacaoAvaliacoes comparacao) {
        return new ComparacaoResposta(comparacao.dataAtual(), comparacao.dataAnterior(), comparacao.itens().stream()
                .map(item -> new DiferencaPorMovimento(item.movimento(), item.repsAnterior(), item.repsAtual(),
                        item.diferenca()))
                .toList());
    }
}
