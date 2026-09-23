package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliacaoFisicaComparacaoServiceTest {

    private final AvaliacaoFisicaComparacaoService service = new AvaliacaoFisicaComparacaoService();
    private final AvaliacaoFisicaCalculoService calculo = new AvaliacaoFisicaCalculoService();

    @Test
    void mostraGanhoPerdaEEstabilidadeNoMesmoConjunto() {
        // anterior: 10, 15, 40, 20, 30, 50
        // atual:    15, 15, 35, 24, 30, 60
        List<AvaliacaoFisicaItemResultado> anterior = calculo.calcular(10, 15, 40, 20, 30, 50);
        List<AvaliacaoFisicaItemResultado> atual = calculo.calcular(15, 15, 35, 24, 30, 60);

        List<ComparacaoItem> comparacao = service.comparar(atual, anterior);

        assertThat(comparacao).containsExactly(
                new ComparacaoItem(MovimentoPadrao.PUXAR_VERTICAL, 10, 15, 5),
                new ComparacaoItem(MovimentoPadrao.EMPURRAR_VERTICAL, 15, 15, 0),
                new ComparacaoItem(MovimentoPadrao.PERNAS_BILATERAL, 40, 35, -5),
                new ComparacaoItem(MovimentoPadrao.PUXAR_HORIZONTAL, 20, 24, 4),
                new ComparacaoItem(MovimentoPadrao.EMPURRAR_HORIZONTAL, 30, 30, 0),
                new ComparacaoItem(MovimentoPadrao.PERNAS_UNILATERAL, 50, 60, 10));
    }

    @Test
    void devolveSempreOsSeisPadroesNaOrdemDoEnum() {
        List<ComparacaoItem> comparacao = service.comparar(
                calculo.calcular(1, 1, 1, 1, 1, 1), calculo.calcular(1, 1, 1, 1, 1, 1));

        assertThat(comparacao).hasSize(6);
        assertThat(comparacao).extracting(ComparacaoItem::movimento)
                .containsExactly(MovimentoPadrao.values());
        assertThat(comparacao).allMatch(item -> item.diferenca() == 0);
    }
}
