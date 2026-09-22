package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Filtro do catalogo e regra de compatibilidade com equipamento, isolados
 * do Controller e do repository pelo mesmo motivo dos outros
 * XCalculoService do projeto: sem Spring nem banco no meio, da pra testar
 * chamando os metodos direto com uma lista construida na mao.
 */
@Service
public class ExercicioFiltroService {

    public List<Exercicio> filtrar(List<Exercicio> catalogo, NivelTreino nivel, MovimentoPadrao movimento,
                                    Equipamento equipamento) {
        return catalogo.stream()
                .filter(exercicio -> nivel == null || exercicio.getNivel() == nivel)
                .filter(exercicio -> movimento == null || exercicio.getMovimento() == movimento)
                .filter(exercicio -> equipamento == null || exercicio.getEquipamentoNecessario() == equipamento)
                .toList();
    }

    public boolean ehCompativel(Exercicio exercicio, Set<Equipamento> equipamentosDisponiveis) {
        return exercicio.getEquipamentoNecessario() == Equipamento.NENHUM
                || equipamentosDisponiveis.contains(exercicio.getEquipamentoNecessario());
    }
}
