package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Equipamentos do usuario, na ordem do enum (resposta estavel pro
 * cliente comparar). */
public record EquipamentosResposta(List<Equipamento> equipamentos) {

    static EquipamentosResposta de(Set<Equipamento> equipamentos) {
        return new EquipamentosResposta(equipamentos.stream().sorted(Comparator.naturalOrder()).toList());
    }
}
