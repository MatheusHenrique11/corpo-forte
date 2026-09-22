package com.corpoforte.tracker.peso;

import java.time.LocalDate;

/**
 * Entrada do calculo de tendencia: so data + peso, nao a entidade JPA.
 * Mantem o RegistroPesoCalculoService sem depender de banco/Spring.
 */
public record RegistroPesoPonto(LocalDate data, double pesoKg) {
}
