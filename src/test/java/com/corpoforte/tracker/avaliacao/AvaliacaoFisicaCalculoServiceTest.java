package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de referencia da formula de volume de treino: repeticoes maximas x6
 * = volume total; 40% do volume total = volume inicial; 5% do volume total
 * = incremento.
 */
class AvaliacaoFisicaCalculoServiceTest {

    private final AvaliacaoFisicaCalculoService service = new AvaliacaoFisicaCalculoService();

    @Test
    void calculaVolumeTotalInicialEIncrementoParaCadaMovimento() {
        List<AvaliacaoFisicaItemResultado> resultado = service.calcular(10, 15, 40, 20, 30, 50);

        assertThat(resultado).containsExactly(
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PUXAR_VERTICAL, 10, 60, 24, 3),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.EMPURRAR_VERTICAL, 15, 90, 36, 5),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PERNAS_BILATERAL, 40, 240, 96, 12),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PUXAR_HORIZONTAL, 20, 120, 48, 6),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.EMPURRAR_HORIZONTAL, 30, 180, 72, 9),
                new AvaliacaoFisicaItemResultado(MovimentoPadrao.PERNAS_UNILATERAL, 50, 300, 120, 15)
        );
    }

    @Test
    void arredondaIncrementoQuandoCincoPorCentoNaoEExato() {
        // 15 reps -> volume total 90 -> 5% = 4.5, arredondado pra 5
        AvaliacaoFisicaItemResultado item = service.calcular(15, 0, 0, 0, 0, 0).get(0);

        assertThat(item.incremento()).isEqualTo(5);
    }
}
