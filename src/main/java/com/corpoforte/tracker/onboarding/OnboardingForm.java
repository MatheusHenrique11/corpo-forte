package com.corpoforte.tracker.onboarding;

import com.corpoforte.tracker.perfil.PerfilForm;
import com.corpoforte.tracker.peso.RegistroPesoForm;
import com.corpoforte.tracker.usuario.UsernameValido;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Tudo que o PerfilForm ja pede (nome, altura, idade, objetivo, nivel -
 * com as mesmas faixas, herdadas) mais username e peso. O peso usa a mesma
 * faixa do registro de peso, porque e' nele que vai parar.
 */
public class OnboardingForm extends PerfilForm {

    @NotBlank(message = "Escolha um nome de usuário")
    @UsernameValido
    private String username;

    @NotNull(message = "Informe o peso")
    @DecimalMin(value = RegistroPesoForm.PESO_MINIMO_KG, message = "Peso minimo: 30 kg")
    @DecimalMax(value = RegistroPesoForm.PESO_MAXIMO_KG, message = "Peso maximo: 300 kg")
    private Double pesoKg;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }
}
