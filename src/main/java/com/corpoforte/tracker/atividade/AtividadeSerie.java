package com.corpoforte.tracker.atividade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma serie feita: qual exercicio, em que ordem dentro da atividade e o
 * valor realizado - repeticoes ou segundos, conforme a medida do
 * exercicio. A medida nao e' copiada aqui: vem do catalogo, como o nome.
 */
@Entity
@Table(name = "atividade_serie")
public class AtividadeSerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atividade_id", nullable = false)
    private Long atividadeId;

    @Column(name = "exercicio_id", nullable = false)
    private Long exercicioId;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private int valor;

    protected AtividadeSerie() {
    }

    public AtividadeSerie(Long atividadeId, Long exercicioId, int ordem, int valor) {
        this.atividadeId = atividadeId;
        this.exercicioId = exercicioId;
        this.ordem = ordem;
        this.valor = valor;
    }

    public Long getId() {
        return id;
    }

    public Long getAtividadeId() {
        return atividadeId;
    }

    public Long getExercicioId() {
        return exercicioId;
    }

    public int getOrdem() {
        return ordem;
    }

    public int getValor() {
        return valor;
    }
}
