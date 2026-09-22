package com.corpoforte.tracker.usuario;

public enum Equipamento {
    NENHUM("Nenhum — peso corporal"),
    BARRA_FIXA("Barra fixa"),
    PARALELAS("Paralelas"),
    ANEIS("Anéis"),
    ELASTICO("Elástico de resistência"),
    BANCO("Banco/step");

    private final String rotulo;

    Equipamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
