package com.corpoforte.tracker.peso;

import java.time.LocalDate;

/** Um dia de pesagem. Dado corporal: so' aparece pro proprio dono. */
public record RegistroPesoResposta(LocalDate data, double pesoKg) {

    static RegistroPesoResposta de(RegistroPeso registro) {
        return new RegistroPesoResposta(registro.getData(), registro.getPesoKg());
    }
}
