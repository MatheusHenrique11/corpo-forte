package com.corpoforte.tracker.avaliacao;

import com.corpoforte.tracker.exercicio.MovimentoPadrao;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Repeticoes maximas (A) vezes 6 vira o volume total de treino (B); 40% de B
 * e' o volume inicial do primeiro treino (C); 5% de B e' o incremento usado
 * na periodizacao mais pra frente (D). Isolado do Controller pelo mesmo
 * motivo do PerfilCalculoService: sem Spring nem banco no meio, da pra
 * testar chamando calcular(...) direto.
 */
@Service
public class AvaliacaoFisicaCalculoService {

    private static final int FATOR_VOLUME_TOTAL = 6;
    private static final double PERCENTUAL_VOLUME_INICIAL = 0.40;
    private static final double PERCENTUAL_INCREMENTO = 0.05;

    public List<AvaliacaoFisicaItemResultado> calcular(
            int repsPuxarVertical,
            int repsEmpurrarVertical,
            int repsPernasBilateral,
            int repsPuxarHorizontal,
            int repsEmpurrarHorizontal,
            int repsPernasUnilateral) {
        return List.of(
                calcularItem(MovimentoPadrao.PUXAR_VERTICAL, repsPuxarVertical),
                calcularItem(MovimentoPadrao.EMPURRAR_VERTICAL, repsEmpurrarVertical),
                calcularItem(MovimentoPadrao.PERNAS_BILATERAL, repsPernasBilateral),
                calcularItem(MovimentoPadrao.PUXAR_HORIZONTAL, repsPuxarHorizontal),
                calcularItem(MovimentoPadrao.EMPURRAR_HORIZONTAL, repsEmpurrarHorizontal),
                calcularItem(MovimentoPadrao.PERNAS_UNILATERAL, repsPernasUnilateral)
        );
    }

    private AvaliacaoFisicaItemResultado calcularItem(MovimentoPadrao movimento, int repeticoesMaximas) {
        int volumeTotalTreino = repeticoesMaximas * FATOR_VOLUME_TOTAL;
        int volumeInicial = (int) Math.round(volumeTotalTreino * PERCENTUAL_VOLUME_INICIAL);
        int incremento = (int) Math.round(volumeTotalTreino * PERCENTUAL_INCREMENTO);
        return new AvaliacaoFisicaItemResultado(movimento, repeticoesMaximas, volumeTotalTreino, volumeInicial, incremento);
    }
}
