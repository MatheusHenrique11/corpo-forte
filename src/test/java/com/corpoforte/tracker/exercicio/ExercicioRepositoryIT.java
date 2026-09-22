package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.IntegrationTestBase;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirma que a migration V5 (seed de dado, nao codigo Java) populou o
 * catalogo do jeito esperado - um erro de digitacao no SQL de seed (nome de
 * enum errado, combinacao faltando) nao apareceria em nenhum teste
 * unitario, so testando contra o banco de verdade.
 */
@Transactional
class ExercicioRepositoryIT extends IntegrationTestBase {

    @Autowired
    private ExercicioRepository exercicioRepository;

    @Test
    void seedPopula54Exercicios() {
        List<Exercicio> todos = exercicioRepository.findAll();

        assertThat(todos).hasSize(54);
    }

    @Test
    void cadaPadraoDeMovimentoTemNoveExerciciosTresPorNivel() {
        List<Exercicio> todos = exercicioRepository.findAll();

        Map<MovimentoPadrao, Long> porMovimento = todos.stream()
                .collect(Collectors.groupingBy(Exercicio::getMovimento, Collectors.counting()));

        assertThat(porMovimento).hasSize(6);
        assertThat(porMovimento.values()).allMatch(quantidade -> quantidade == 9);
    }

    @Test
    void cadaNivelTem18ExerciciosDoisPorPadraoDeMovimento() {
        List<Exercicio> todos = exercicioRepository.findAll();

        Map<NivelTreino, Long> porNivel = todos.stream()
                .collect(Collectors.groupingBy(Exercicio::getNivel, Collectors.counting()));

        assertThat(porNivel).hasSize(3);
        assertThat(porNivel.values()).allMatch(quantidade -> quantidade == 18);
    }

    @Test
    void nenhumExercicioFicaSemEquipamentoDefinido() {
        List<Exercicio> todos = exercicioRepository.findAll();

        assertThat(todos).extracting(Exercicio::getEquipamentoNecessario).doesNotContainNull();
        assertThat(todos).extracting(Exercicio::getNome).doesNotContainNull();
    }

    @Test
    void muscleUpEstaCadastradoComoAvancadoDePuxarVerticalComBarraFixa() {
        List<Exercicio> todos = exercicioRepository.findAll();

        assertThat(todos)
                .filteredOn(exercicio -> exercicio.getNome().equals("Muscle-up"))
                .singleElement()
                .satisfies(exercicio -> {
                    assertThat(exercicio.getMovimento()).isEqualTo(MovimentoPadrao.PUXAR_VERTICAL);
                    assertThat(exercicio.getNivel()).isEqualTo(NivelTreino.AVANCADO);
                    assertThat(exercicio.getEquipamentoNecessario()).isEqualTo(Equipamento.BARRA_FIXA);
                });
    }
}
