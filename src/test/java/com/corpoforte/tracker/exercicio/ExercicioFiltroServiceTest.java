package com.corpoforte.tracker.exercicio;

import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExercicioFiltroServiceTest {

    private final ExercicioFiltroService service = new ExercicioFiltroService();

    private final List<Exercicio> catalogo = List.of(
            new Exercicio("Agachamento livre", MovimentoPadrao.PERNAS_BILATERAL, NivelTreino.INICIANTE, Equipamento.NENHUM),
            new Exercicio("Agachamento bulgaro", MovimentoPadrao.PERNAS_BILATERAL, NivelTreino.AVANCADO, Equipamento.BANCO),
            new Exercicio("Barra fixa completa", MovimentoPadrao.PUXAR_VERTICAL, NivelTreino.INTERMEDIARIO, Equipamento.BARRA_FIXA),
            new Exercicio("Remada em aneis", MovimentoPadrao.PUXAR_HORIZONTAL, NivelTreino.INICIANTE, Equipamento.ANEIS)
    );

    @Test
    void semFiltroDevolveOCatalogoInteiro() {
        assertThat(service.filtrar(catalogo, null, null, null)).hasSize(4);
    }

    @Test
    void filtraPorNivelIsolado() {
        assertThat(service.filtrar(catalogo, NivelTreino.INICIANTE, null, null))
                .extracting(Exercicio::getNome)
                .containsExactlyInAnyOrder("Agachamento livre", "Remada em aneis");
    }

    @Test
    void filtraPorMovimentoEEquipamentoCombinados() {
        assertThat(service.filtrar(catalogo, null, MovimentoPadrao.PERNAS_BILATERAL, Equipamento.BANCO))
                .extracting(Exercicio::getNome)
                .containsExactly("Agachamento bulgaro");
    }

    @Test
    void exercicioSemEquipamentoEhSempreCompativel() {
        Exercicio semEquipamento = catalogo.get(0);

        assertThat(service.ehCompativel(semEquipamento, Set.of())).isTrue();
    }

    @Test
    void exercicioComEquipamentoSoEhCompativelSeUsuarioTiver() {
        Exercicio precisaDeBarra = catalogo.get(2);

        assertThat(service.ehCompativel(precisaDeBarra, Set.of(Equipamento.BARRA_FIXA))).isTrue();
        assertThat(service.ehCompativel(precisaDeBarra, Set.of(Equipamento.ANEIS))).isFalse();
        assertThat(service.ehCompativel(precisaDeBarra, Set.of())).isFalse();
    }
}
