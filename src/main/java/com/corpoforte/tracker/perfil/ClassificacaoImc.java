package com.corpoforte.tracker.perfil;

public enum ClassificacaoImc {
    ABAIXO_DO_PESO("Abaixo do peso"),
    NORMAL("Peso normal"),
    SOBREPESO("Sobrepeso"),
    OBESIDADE_GRAU_I("Obesidade grau I"),
    OBESIDADE_GRAU_II("Obesidade grau II"),
    OBESIDADE_GRAU_III("Obesidade grau III");

    private final String rotulo;

    ClassificacaoImc(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public static ClassificacaoImc classificar(double imc) {
        if (imc < 18.5) return ABAIXO_DO_PESO;
        if (imc < 25) return NORMAL;
        if (imc < 30) return SOBREPESO;
        if (imc < 35) return OBESIDADE_GRAU_I;
        if (imc < 40) return OBESIDADE_GRAU_II;
        return OBESIDADE_GRAU_III;
    }
}
