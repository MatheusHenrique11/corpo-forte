package com.corpoforte.tracker.avaliacao;

public enum MovimentoPadrao {
    PUXAR_VERTICAL("Puxar vertical"),
    EMPURRAR_VERTICAL("Empurrar vertical"),
    PERNAS_BILATERAL("Pernas bilateral"),
    PUXAR_HORIZONTAL("Puxar horizontal"),
    EMPURRAR_HORIZONTAL("Empurrar horizontal"),
    PERNAS_UNILATERAL("Pernas unilateral");

    private final String rotulo;

    MovimentoPadrao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
