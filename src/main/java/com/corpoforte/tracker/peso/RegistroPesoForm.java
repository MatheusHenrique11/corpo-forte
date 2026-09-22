package com.corpoforte.tracker.peso;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class RegistroPesoForm {

    @NotNull(message = "Informe a data")
    @PastOrPresent(message = "A data nao pode ser no futuro")
    private LocalDate data = LocalDate.now();

    @NotNull(message = "Informe o peso")
    @DecimalMin(value = "30.0", message = "Peso minimo: 30 kg")
    @DecimalMax(value = "300.0", message = "Peso maximo: 300 kg")
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
