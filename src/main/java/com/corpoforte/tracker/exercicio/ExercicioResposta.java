package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;

/** Item do catalogo. id e' estavel: e' por ele que o treino do dia
 * referencia o exercicio. */
public record ExercicioResposta(Long id, String nome, MovimentoPadrao movimento, NivelTreino nivel,
                                Equipamento equipamentoNecessario, Medida medida) {

    static ExercicioResposta de(Exercicio exercicio) {
        return new ExercicioResposta(exercicio.getId(), exercicio.getNome(), exercicio.getMovimento(),
                exercicio.getNivel(), exercicio.getEquipamentoNecessario(), exercicio.getMedida());
    }
}
