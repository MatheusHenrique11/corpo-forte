package com.corpoforte.tracker.peso;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class RegistroPesoForm {

    /** Faixa de peso aceita em todo lugar que recebe peso (registro e
     * onboarding) - uma constante, pra as duas validacoes nao divergirem. */
    public static final String PESO_MINIMO_KG = "30.0";
    public static final String PESO_MAXIMO_KG = "300.0";

    @NotNull(message = "Informe a data")
    @PastOrPresent(message = "A data nao pode ser no futuro")
    private LocalDate data = LocalDate.now();

    @NotNull(message = "Informe o peso")
    @DecimalMin(value = PESO_MINIMO_KG, message = "Peso minimo: 30 kg")
    @DecimalMax(value = PESO_MAXIMO_KG, message = "Peso maximo: 300 kg")
    private Double pesoKg;

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }
}
