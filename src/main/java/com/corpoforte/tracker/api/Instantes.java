package com.corpoforte.tracker.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * O banco guarda criado_em como timestamp sem fuso, no horario do servidor.
 * Na API todo instante sai como Instant (UTC, "...Z"): um horario sem fuso
 * e' ambiguo pra um cliente em outro fuso, e trocar o formato depois seria
 * mudanca incompativel. Datas de calendario (dia do peso, da avaliacao)
 * continuam LocalDate - "23/09" nao tem fuso.
 */
public final class Instantes {

    private Instantes() {
    }

    public static Instant emUtc(LocalDateTime horarioDoServidor) {
        return horarioDoServidor.atZone(ZoneId.systemDefault()).toInstant();
    }
}
