package com.corpoforte.tracker.avaliacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO ligado ao <form> do Thymeleaf, mesmo papel do PerfilForm: a regra de
 * negocio "quantas repeticoes sao um numero plausivel" fica aqui, nao
 * espalhada no Controller ou no Service.
 */
public class AvaliacaoFisicaForm {

    @NotNull(message = "Informe o resultado do teste")
    @Min(value = 1, message = "Minimo: 1 repeticao")
    @Max(value = 150, message = "Maximo: 150 repeticoes")
    private Integer repsPuxarVertical;

    @NotNull(message = "Informe o resultado do teste")
    @Min(value = 1, message = "Minimo: 1 repeticao")
    @Max(value = 150, message = "Maximo: 150 repeticoes")
    private Integer repsEmpurrarVertical;

    @NotNull(message = "Informe o resultado do teste")
    @Min(value = 1, message = "Minimo: 1 repeticao")
    @Max(value = 150, message = "Maximo: 150 repeticoes")
    private Integer repsPernasBilateral;

    @NotNull(message = "Informe o resultado do teste")
    @Min(value = 1, message = "Minimo: 1 repeticao")
    @Max(value = 150, message = "Maximo: 150 repeticoes")
    private Integer repsPuxarHorizontal;

    @NotNull(message = "Informe o resultado do teste")
    @Min(value = 1, message = "Minimo: 1 repeticao")
    @Max(value = 150, message = "Maximo: 150 repeticoes")
    private Integer repsEmpurrarHorizontal;

    @NotNull(message = "Informe o resultado do teste")
    @Min(value = 1, message = "Minimo: 1 repeticao")
    @Max(value = 150, message = "Maximo: 150 repeticoes")
    private Integer repsPernasUnilateral;

    public Integer getRepsPuxarVertical() {
        return repsPuxarVertical;
    }

    public void setRepsPuxarVertical(Integer repsPuxarVertical) {
        this.repsPuxarVertical = repsPuxarVertical;
    }

    public Integer getRepsEmpurrarVertical() {
        return repsEmpurrarVertical;
    }

    public void setRepsEmpurrarVertical(Integer repsEmpurrarVertical) {
        this.repsEmpurrarVertical = repsEmpurrarVertical;
    }

    public Integer getRepsPernasBilateral() {
        return repsPernasBilateral;
    }

    public void setRepsPernasBilateral(Integer repsPernasBilateral) {
        this.repsPernasBilateral = repsPernasBilateral;
    }

    public Integer getRepsPuxarHorizontal() {
        return repsPuxarHorizontal;
    }

    public void setRepsPuxarHorizontal(Integer repsPuxarHorizontal) {
        this.repsPuxarHorizontal = repsPuxarHorizontal;
    }

    public Integer getRepsEmpurrarHorizontal() {
        return repsEmpurrarHorizontal;
    }

    public void setRepsEmpurrarHorizontal(Integer repsEmpurrarHorizontal) {
        this.repsEmpurrarHorizontal = repsEmpurrarHorizontal;
    }

    public Integer getRepsPernasUnilateral() {
        return repsPernasUnilateral;
    }

    public void setRepsPernasUnilateral(Integer repsPernasUnilateral) {
        this.repsPernasUnilateral = repsPernasUnilateral;
    }
}
