package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;

import java.time.LocalDate;
import java.util.List;

/**
 * Treino do dia com o checklist. movimentosSemOpcao: padroes que ficaram
 * de fora porque nenhum exercicio do nivel do usuario cabe nos
 * equipamentos que ele tem (Fase 5). ciclo: semana atual da periodizacao
 * e se ja e' hora de refazer a avaliacao (Fase 8).
 */
public record TreinoDoDiaResposta(LocalDate data, List<ItemResposta> itens, List<MovimentoPadrao> movimentosSemOpcao,
                                  CicloResposta ciclo) {

    /** id e' o que vai na rota de conclusao do item. */
    public record ItemResposta(Long id, MovimentoPadrao movimento, ExercicioDoItem exercicio, int series,
                               int repeticoes, boolean concluido) {
    }

    public record ExercicioDoItem(Long id, String nome) {
    }

    public record CicloResposta(int semanaAtual, int totalSemanas, boolean precisaReavaliar) {
    }

    static TreinoDoDiaResposta de(TreinoDoDiaView treino) {
        return new TreinoDoDiaResposta(treino.data(),
                treino.itens().stream()
                        .map(item -> new ItemResposta(item.itemId(), item.movimento(),
                                new ExercicioDoItem(item.exercicioId(), item.exercicioNome()),
                                item.series(), item.repeticoes(), item.concluido()))
                        .toList(),
                treino.movimentosSemOpcao(),
                new CicloResposta(treino.ciclo().semanaAtual(), treino.ciclo().totalSemanas(),
                        treino.ciclo().precisaReavaliar()));
    }
}
