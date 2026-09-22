package com.corpoforte.tracker.peso;

public enum DirecaoTendencia {
    SUBINDO("Subindo"),
    DESCENDO("Descendo"),
    ESTAVEL("Estável"),
    SEM_COMPARATIVO("Ainda sem semana anterior pra comparar");

    private final String rotulo;

    DirecaoTendencia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
