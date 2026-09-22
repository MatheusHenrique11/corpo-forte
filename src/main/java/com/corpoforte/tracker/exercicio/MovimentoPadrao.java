package com.corpoforte.tracker.exercicio;

/**
 * Taxonomia usada pra categorizar exercicios (Fase 4). A avaliacao fisica
 * da Fase 2 toma emprestado esses mesmos 6 padroes pro teste de repeticoes
 * maximas - por isso o enum mora aqui, no pacote "dono" do conceito, e
 * avaliacao que importa daqui, nao o contrario.
 */
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
