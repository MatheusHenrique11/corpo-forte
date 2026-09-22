package com.corpoforte.tracker.treino;

import com.corpoforte.tracker.avaliacao.AvaliacaoFisicaItemResultado;
import com.corpoforte.tracker.exercicio.Exercicio;
import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import com.corpoforte.tracker.usuario.Equipamento;
import com.corpoforte.tracker.usuario.NivelTreino;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TreinoDoDiaGeradorServiceTest {

    private final TreinoDoDiaGeradorService service = new TreinoDoDiaGeradorService();

    private List<AvaliacaoFisicaItemResultado> volumesComVolumeInicial(int volumeInicial) {
        return List.of(
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PUXAR_VERTICAL, 10, 60, volumeInicial, 3),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.EMPURRAR_VERTICAL, 10, 60, volumeInicial, 3),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PERNAS_BILATERAL, 10, 60, volumeInicial, 3),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PUXAR_HORIZONTAL, 10, 60, volumeInicial, 3),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.EMPURRAR_HORIZONTAL, 10, 60, volumeInicial, 3),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PERNAS_UNILATERAL, 10, 60, volumeInicial, 3)
        );
    }

    @Test
    void geraUmItemPorPadraoComUnicoCandidatoDisponivel() {
        Exercicio agachamento = new Exercicio("Agachamento livre", MovimentoPadrao.PERNAS_BILATERAL,
                NivelTreino.INICIANTE, Equipamento.NENHUM);

        Map<MovimentoPadrao, List<Exercicio>> candidatos = new EnumMap<>(MovimentoPadrao.class);
        for (MovimentoPadrao movimento : MovimentoPadrao.values()) {
            candidatos.put(movimento, movimento == MovimentoPadrao.PERNAS_BILATERAL
                    ? List.of(agachamento) : List.of());
        }
        candidatos.put(MovimentoPadrao.PERNAS_BILATERAL, List.of(agachamento));

        TreinoGerado resultado = service.gerar(volumesComVolumeInicial(24), candidatos);

        assertThat(resultado.itens()).hasSize(1);
        ItemGerado item = resultado.itens().get(0);
        assertThat(item.movimento()).isEqualTo(MovimentoPadrao.PERNAS_BILATERAL);
        assertThat(item.exercicio()).isEqualTo(agachamento);
        assertThat(item.series()).isEqualTo(3);
        assertThat(item.repeticoes()).isEqualTo(8); // 24 / 3

        assertThat(resultado.movimentosSemOpcao()).hasSize(5);
        assertThat(resultado.movimentosSemOpcao()).doesNotContain(MovimentoPadrao.PERNAS_BILATERAL);
    }

    @Test
    void padraoSemCandidatoNenhumViraSemOpcaoSemQuebrarOsDemais() {
        Exercicio unico = new Exercicio("Flexao de joelhos", MovimentoPadrao.EMPURRAR_HORIZONTAL,
                NivelTreino.INICIANTE, Equipamento.NENHUM);

        Map<MovimentoPadrao, List<Exercicio>> candidatos = new EnumMap<>(MovimentoPadrao.class);
        for (MovimentoPadrao movimento : MovimentoPadrao.values()) {
            candidatos.put(movimento, List.of());
        }
        candidatos.put(MovimentoPadrao.EMPURRAR_HORIZONTAL, List.of(unico));

        TreinoGerado resultado = service.gerar(volumesComVolumeInicial(30), candidatos);

        assertThat(resultado.itens()).hasSize(1);
        assertThat(resultado.itens().get(0).exercicio()).isEqualTo(unico);
        assertThat(resultado.movimentosSemOpcao()).containsExactlyInAnyOrder(
                MovimentoPadrao.PUXAR_VERTICAL, MovimentoPadrao.EMPURRAR_VERTICAL,
                MovimentoPadrao.PERNAS_BILATERAL, MovimentoPadrao.PUXAR_HORIZONTAL,
                MovimentoPadrao.PERNAS_UNILATERAL);
    }

    @Test
    void arredondaRepeticoesQuandoVolumeInicialNaoEDivisivelPorTres() {
        Exercicio unico = new Exercicio("Remada australiana", MovimentoPadrao.PUXAR_HORIZONTAL,
                NivelTreino.INICIANTE, Equipamento.ANEIS);

        Map<MovimentoPadrao, List<Exercicio>> candidatos = new EnumMap<>(MovimentoPadrao.class);
        for (MovimentoPadrao movimento : MovimentoPadrao.values()) {
            candidatos.put(movimento, List.of());
        }
        candidatos.put(MovimentoPadrao.PUXAR_HORIZONTAL, List.of(unico));

        // volume inicial 25 -> 25/3 = 8.33 -> arredonda pra 8
        TreinoGerado resultado = service.gerar(volumesComVolumeInicial(25), candidatos);

        assertThat(resultado.itens().get(0).repeticoes()).isEqualTo(8);
    }
}
