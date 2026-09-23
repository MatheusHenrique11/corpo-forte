package com.corpoforte.tracker.treino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Um exercicio dentro do treino do dia (checklist). exercicioId referencia
 * o catalogo (Exercicio) sem duplicar nome/padrao - evita divergir da
 * fonte se o catalogo mudar depois. treinoDoDiaId e' Long solto, mesmo
 * padrao do resto do projeto (sem @ManyToOne).
 */
@Entity
@Table(name = "treino_item")
public class TreinoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "treino_do_dia_id", nullable = false)
    private Long treinoDoDiaId;

    @Column(name = "exercicio_id", nullable = false)
    private Long exercicioId;

    @Column(nullable = false)
    private int series;

    @Column(nullable = false)
    private int repeticoes;

    @Column(nullable = false)
    private boolean concluido = false;

    protected TreinoItem() {
    }

    public TreinoItem(Long treinoDoDiaId, Long exercicioId, int series, int repeticoes) {
        this.treinoDoDiaId = treinoDoDiaId;
        this.exercicioId = exercicioId;
        this.series = series;
        this.repeticoes = repeticoes;
    }

    public void alternarConclusao() {
        this.concluido = !this.concluido;
    }

    public void definirConclusao(boolean concluido) {
        this.concluido = concluido;
    }

    public Long getId() {
        return id;
    }

    public Long getTreinoDoDiaId() {
        return treinoDoDiaId;
    }

    public Long getExercicioId() {
        return exercicioId;
    }

    public int getSeries() {
        return series;
    }

    public int getRepeticoes() {
        return repeticoes;
    }

    public boolean isConcluido() {
        return concluido;
    }
}
