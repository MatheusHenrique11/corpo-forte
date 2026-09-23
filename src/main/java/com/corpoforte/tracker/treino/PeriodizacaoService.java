package com.corpoforte.tracker.treino;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

/**
 * Quanto o volume ja progrediu no ciclo atual. Progride por SEMANA EM QUE
 * HOUVE TREINO CONCLUIDO, nao por calendario: quem parou tres semanas volta
 * de onde parou, nao num volume que nunca treinou pra alcancar.
 *
 * So conta semanas ESTRITAMENTE ANTERIORES a semana corrente, pro volume
 * ficar estavel dentro da semana (nao pular no meio dela quando o usuario
 * conclui o primeiro treino) - coerente com "o treino do dia fica fixo ate
 * amanha" da Fase 5.
 *
 * Duas linhas do tempo diferentes, de proposito:
 * - semanaAtual (tela)    -> esforco, casa com o volume que a pessoa recebe
 * - precisaReavaliar      -> calendario, 8 semanas desde a avaliacao
 * Quem treinou 2 de 8 semanas ve "Semana 3 de 8" e ao mesmo tempo o aviso
 * de refazer a avaliacao: o contador reflete esforco, mas o baseline
 * envelhece no relogio - quem sumiu por meses provavelmente perdeu
 * capacidade, e manter a medicao antiga seria pior que a inconsistencia.
 *
 * Sem Spring/banco no construtor: da pra testar chamando os metodos direto,
 * mesmo molde dos outros XCalculoService do projeto.
 */
@Service
public class PeriodizacaoService {

    private static final int SEMANAS_DO_CICLO = 8;
    private static final int MAXIMO_DE_SEMANAS_PROGREDIDAS = SEMANAS_DO_CICLO - 1;

    public int semanasProgredidas(Set<LocalDate> semanasComTreinoConcluido, LocalDate hoje) {
        LocalDate semanaCorrente = inicioDaSemana(hoje);

        long anteriores = semanasComTreinoConcluido.stream()
                .filter(semana -> semana.isBefore(semanaCorrente))
                .count();

        return (int) Math.min(anteriores, MAXIMO_DE_SEMANAS_PROGREDIDAS);
    }

    public CicloStatus status(LocalDate dataAvaliacao, LocalDate hoje, int semanasProgredidas) {
        boolean precisaReavaliar = !hoje.isBefore(dataAvaliacao.plusWeeks(SEMANAS_DO_CICLO));

        return new CicloStatus(semanasProgredidas + 1, SEMANAS_DO_CICLO, precisaReavaliar);
    }

    /**
     * Semana comeca na segunda em todo o projeto - mesma convencao de
     * RegistroPesoCalculoService (Fase 3). Se as duas divergirem, os numeros
     * semanais do app passam a discordar entre si.
     */
    public LocalDate inicioDaSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
