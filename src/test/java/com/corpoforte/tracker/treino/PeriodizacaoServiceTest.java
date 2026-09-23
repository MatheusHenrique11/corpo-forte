package com.corpoforte.tracker.treino;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Semanas de referencia (todas segundas-feiras, ver
 * PeriodizacaoService.inicioDaSemana): 01/06/2026, 08/06, 15/06, 22/06,
 * 29/06 - as mesmas datas ja usadas em RegistroPesoCalculoServiceTest, pra
 * manter a convencao de semana obvia entre os dois testes.
 */
class PeriodizacaoServiceTest {

    private final PeriodizacaoService service = new PeriodizacaoService();

    private static final LocalDate SEMANA_1 = LocalDate.of(2026, 6, 1);
    private static final LocalDate SEMANA_2 = LocalDate.of(2026, 6, 8);
    private static final LocalDate SEMANA_3 = LocalDate.of(2026, 6, 15);
    private static final LocalDate SEMANA_5 = LocalDate.of(2026, 6, 29);

    @Test
    void semNenhumTreinoConcluidoOVolumeNaoProgride() {
        assertThat(service.semanasProgredidas(Set.of(), SEMANA_1)).isZero();
    }

    @Test
    void duasSemanasAnterioresComTreinoValemDoisIncrementos() {
        int progredidas = service.semanasProgredidas(Set.of(SEMANA_1, SEMANA_2), SEMANA_3.plusDays(2));

        assertThat(progredidas).isEqualTo(2);
    }

    /**
     * O caso que prova a decisao de produto desta fase: treinou nas semanas
     * 1 e 2, sumiu nas 3 e 4, voltou na 5. Progride 2 (o que treinou), nao 4
     * (o que o calendario andou) - volta de onde parou, nao num volume que
     * nunca treinou pra alcancar.
     */
    @Test
    void semanasSemTreinoNaoContamMesmoComOCalendarioAndando() {
        int progredidas = service.semanasProgredidas(Set.of(SEMANA_1, SEMANA_2), SEMANA_5);

        assertThat(progredidas).isEqualTo(2);
    }

    @Test
    void aSemanaCorrenteNaoContaParaOVolumeFicarEstavelDentroDela() {
        // concluiu treino nesta mesma semana: o volume de hoje nao muda
        int progredidas = service.semanasProgredidas(Set.of(SEMANA_1, SEMANA_2), SEMANA_2.plusDays(3));

        assertThat(progredidas).isEqualTo(1);
    }

    @Test
    void progressaoTemTetoNaOitavaSemanaDoCiclo() {
        Set<LocalDate> vinteSemanas = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) {
            vinteSemanas.add(SEMANA_1.plusWeeks(i));
        }

        int progredidas = service.semanasProgredidas(vinteSemanas, SEMANA_1.plusWeeks(30));

        assertThat(progredidas).isEqualTo(7); // semana 8 de 8
    }

    @Test
    void cicloNaoPedeReavaliacaoAntesDasOitoSemanas() {
        CicloStatus status = service.status(SEMANA_1, SEMANA_1.plusWeeks(7), 3);

        assertThat(status.semanaAtual()).isEqualTo(4);
        assertThat(status.totalSemanas()).isEqualTo(8);
        assertThat(status.precisaReavaliar()).isFalse();
    }

    @Test
    void cicloPedeReavaliacaoQuandoOCalendarioPassaDasOitoSemanas() {
        CicloStatus status = service.status(SEMANA_1, SEMANA_1.plusWeeks(8), 7);

        assertThat(status.precisaReavaliar()).isTrue();
    }

    /**
     * As duas linhas do tempo divergindo de proposito: treinou pouco
     * (semana 3 de esforco) mas o baseline ja tem 8 semanas de idade.
     */
    @Test
    void contadorDeEsforcoEAvisoDeReavaliacaoSaoIndependentes() {
        CicloStatus status = service.status(SEMANA_1, SEMANA_1.plusWeeks(9), 2);

        assertThat(status.semanaAtual()).isEqualTo(3);
        assertThat(status.precisaReavaliar()).isTrue();
    }
}
